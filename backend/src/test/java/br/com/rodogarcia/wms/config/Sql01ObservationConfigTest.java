package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.json.JsonMapper;

/** Somente objetos locais/servlet mock/arquivos efemeros; nenhum DataSource ou servidor. */
class Sql01ObservationConfigTest {
    @TempDir Path temporary;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void sanitizeNaoConservaLiteraisComentariosOuInterrogacoesFalsas() {
        String sql =
                "select a1_0.id from [segredo?]]x] a1_0 where a1_0.x=? and a1_0.y=N'segredo''?' "
                        + "and a1_0.z=0xDEADBEEF and a1_0.p=.75 /* externo /* interno */ segredo-final? */ "
                        + "and a1_0.q=42 -- senha?\n and a1_0.r=?";
        var result = Sql01ObservationConfig.sanitizar(sql);
        assertThat(result.parameters()).isEqualTo(2);
        assertThat(result.shape())
                .isEqualTo(
                        "select a1_0.id from <identifier> a1_0 where a1_0.x=? and a1_0.y=N<literal> and a1_0.z=<number> and a1_0.p=<number> and a1_0.q=<number> and a1_0.r=?");
    }

    @Test
    void retornaSqlOriginalEIgnoraStatementsForaDoRequest() throws Exception {
        var observer = new Sql01ObservationConfig.Observador(temporary, mapper);
        String sql = "select x from wms.t where id=?";
        assertThat(observer.inspect(sql)).isSameAs(sql);
        observer.terminar("/api/{unresolved}", 200, true, false);
        try (var files = Files.list(temporary)) {
            assertThat(files.count()).isZero();
        }
    }

    @Test
    void contaStatementsParametrosETxSemInferirExecucaoJdbc() throws Exception {
        var observer = new Sql01ObservationConfig.Observador(temporary, mapper);
        String id = UUID.randomUUID().toString();
        observer.iniciar(id, "GET");
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
            TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(
                    Connection.TRANSACTION_SERIALIZABLE);
            observer.inspect("select x from wms.t where a=? and b=?");
            observer.inspect("select x from wms.t where a=? and b=?");
            observer.inspect("select x from wms.t where a=?");
            observer.terminar("/api/v1/produtos", 200, true, false);
        } finally {
            TransactionSynchronizationManager.clear();
        }
        var result = mapper.readTree(Files.readString(temporary.resolve(id + ".json")));
        assertThat(result.get("statementsInspectedPrepared").asInt()).isEqualTo(3);
        assertThat(result.get("parameterPlaceholdersObserved").asInt()).isEqualTo(5);
        assertThat(result.get("maxParameterPlaceholdersObserved").asInt()).isEqualTo(2);
        assertThat(result.get("springReadOnlyStatements").asInt()).isEqualTo(3);
        assertThat(result.get("springTransactionActiveStatements").asInt()).isEqualTo(3);
        assertThat(result.get("springIsolationLevels").get(0).asInt()).isEqualTo(8);
        assertThat(result.get("sanitizedSqlShapes").size()).isEqualTo(2);
        assertThat(result.has("rows")).isFalse();
        assertThat(result.has("sqlCpu")).isFalse();
        assertThat(result.get("limit").asString()).contains("before JDBC execution");
    }

    @Test
    void requestsConcorrentesNaoContaminamContagens() throws Exception {
        var observer = new Sql01ObservationConfig.Observador(temporary, mapper);
        var ready = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> medirConcorrente(observer, ready, release, 1));
            var second = executor.submit(() -> medirConcorrente(observer, ready, release, 7));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            for (var item :
                    Map.of(first.get(5, TimeUnit.SECONDS), 1, second.get(5, TimeUnit.SECONDS), 7)
                            .entrySet()) {
                var result =
                        mapper.readTree(
                                Files.readString(temporary.resolve(item.getKey() + ".json")));
                assertThat(result.get("statementsInspectedPrepared").asInt())
                        .isEqualTo(item.getValue());
            }
        } finally {
            release.countDown();
        }
    }

    private String medirConcorrente(
            Sql01ObservationConfig.Observador observer,
            CountDownLatch ready,
            CountDownLatch release,
            int count)
            throws Exception {
        String id = UUID.randomUUID().toString();
        observer.iniciar(id, "GET");
        ready.countDown();
        if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("barreira local");
        for (int i = 0; i < count; i++) observer.inspect("select x from wms.t where id=?");
        observer.terminar("/api/v1/produtos", 200, true, false);
        return id;
    }

    @Test
    void createOnlyPreservaArquivoEContextoELimitesSaoExplicitos() throws Exception {
        var observer = new Sql01ObservationConfig.Observador(temporary, mapper);
        String id = UUID.randomUUID().toString();
        Path output = temporary.resolve(id + ".json");
        Files.writeString(output, "anterior");
        observer.iniciar(id, "GET");
        observer.inspect("select x from wms.t");
        assertThatThrownBy(() -> observer.terminar("/api/v1/produtos", 200, true, false))
                .isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThat(Files.readString(output)).isEqualTo("anterior");
        String next = UUID.randomUUID().toString();
        observer.iniciar(next, "GET");
        for (int i = 0; i < 260; i++)
            observer.inspect("select campo_" + i + " from wms.t where id=?");
        observer.terminar("/api/v1/produtos", 200, true, false);
        var result = mapper.readTree(Files.readString(temporary.resolve(next + ".json")));
        assertThat(result.get("statementsInspectedPrepared").asInt()).isEqualTo(260);
        assertThat(result.get("sanitizedSqlShapes").size()).isEqualTo(256);
        assertThat(result.get("shapesOmitted").asInt()).isEqualTo(4);
    }

    @Test
    void filtroNaoRegistraUriQueryHeaderBodyMetodoArbitrarioOuErro() throws Exception {
        var observer = new Sql01ObservationConfig.Observador(temporary, mapper);
        var filter = new Sql01ObservationConfig.Filtro(observer);
        var request = new MockHttpServletRequest("SEGREDO_METODO", "/api/v1/produtos");
        request.setQueryString("senha=SEGREDO_QUERY");
        request.addHeader("Authorization", "SEGREDO_TOKEN");
        request.addHeader(IdentificacaoOperacaoFilter.HEADER, "SEGREDO_HEADER");
        request.setContent("SEGREDO_BODY".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = new MockHttpServletResponse();
        var original = new ServletException("SEGREDO_ERRO");
        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        request,
                                        response,
                                        (req, res) -> {
                                            observer.inspect(
                                                    "select x from wms.t where segredo='SEGREDO_LITERAL'");
                                            response.setStatus(403);
                                            throw original;
                                        }))
                .isSameAs(original);
        try (var files = Files.list(temporary)) {
            String json = Files.readString(files.findFirst().orElseThrow());
            assertThat(json).doesNotContain("SEGREDO_");
            var result = mapper.readTree(json);
            assertThat(result.get("method").asString()).isEqualTo("OTHER");
            assertThat(result.get("routePattern").asString()).isEqualTo("/api/{unresolved}");
            assertThat(result.get("httpStatus").asInt()).isEqualTo(403);
            assertThat(result.get("chainCompleted").asBoolean()).isFalse();
        }
    }

    @Test
    void filtroPreservaResponseMesmoQuandoEvidenciaRecusadaENaoObservaLogin() throws Exception {
        var observer = new Sql01ObservationConfig.Observador(temporary, mapper);
        var filter = new Sql01ObservationConfig.Filtro(observer);
        String id = UUID.randomUUID().toString();
        Files.writeString(temporary.resolve(id + ".json"), "anterior");
        var request = new MockHttpServletRequest("GET", "/api/auth/eu");
        request.setAttribute(IdentificacaoOperacaoFilter.ATRIBUTO, id);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/auth/eu");
        var response = new MockHttpServletResponse();
        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    observer.inspect("select x from wms.t");
                    response.setStatus(202);
                });
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(Files.readString(temporary.resolve(id + ".json"))).isEqualTo("anterior");
        for (String route :
                new String[] {
                    "/api/auth/entrar", "/api/auth/senha", "/api/auth/renovar", "/fora"
                }) {
            filter.doFilter(
                    new MockHttpServletRequest("POST", route),
                    response,
                    (req, res) -> observer.inspect("select x from wms.t"));
        }
        try (var files = Files.list(temporary)) {
            assertThat(files.count()).isEqualTo(1);
        }
    }

    @Test
    void guardaDeSaidaExigePosseDiretorioExistenteESubdiretorioExclusivo() throws Exception {
        Path allowed =
                Files.createDirectories(
                        temporary.resolve("orchestracao/.runtime/qual-conf01/sql01/cedro"));
        Path output = Files.createDirectory(allowed.resolve("novo"));
        var environment =
                new MockEnvironment()
                        .withProperty("wms.sql01.repository-root", temporary.toString())
                        .withProperty("wms.sql01.observation.dir", output.toString());
        assertThat(Sql01ObservationConfig.validarDiretorio(environment))
                .isEqualTo(output.toRealPath());
        assertThatThrownBy(() -> Sql01ObservationConfig.validarDiretorio(new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class);
        environment.setProperty("wms.sql01.observation.dir", temporary.toString());
        assertThatThrownBy(() -> Sql01ObservationConfig.validarDiretorio(environment))
                .isInstanceOf(IllegalStateException.class);
        environment.setProperty("wms.sql01.observation.dir", allowed.toString());
        assertThatThrownBy(() -> Sql01ObservationConfig.validarDiretorio(environment))
                .isInstanceOf(IllegalStateException.class);
        environment.setProperty("wms.sql01.observation.dir", allowed.resolve("ausente").toString());
        assertThatThrownBy(() -> Sql01ObservationConfig.validarDiretorio(environment))
                .isInstanceOf(java.nio.file.NoSuchFileException.class);
    }

    @Test
    void configuracaoAusentePorPadraoOuForaSqlDevSemDataSource() throws Exception {
        for (boolean sqlDev : new boolean[] {true, false}) {
            try (var context = new AnnotationConfigApplicationContext()) {
                if (sqlDev) context.getEnvironment().setActiveProfiles("sqlserver-dev");
                else
                    context.getEnvironment()
                            .getPropertySources()
                            .addFirst(
                                    new org.springframework.core.env.MapPropertySource(
                                            "ficticio",
                                            Map.of("wms.sql01.observation.enabled", "true")));
                context.register(Sql01ObservationConfig.class);
                context.refresh();
                assertThat(context.getBeansOfType(Sql01ObservationConfig.class)).isEmpty();
                assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
            }
        }
    }

    @Test
    void customizerNaoSubstituiInspectorExistenteEFilterSegueIdentificacao() throws Exception {
        Path allowed =
                Files.createDirectories(
                        temporary.resolve("orchestracao/.runtime/qual-conf01/sql01/cedro/novo"));
        var environment =
                new MockEnvironment()
                        .withProperty("wms.sql01.repository-root", temporary.toString())
                        .withProperty("wms.sql01.observation.dir", allowed.toString());
        var config = new Sql01ObservationConfig(environment, mapper);
        var observer = config.sql01Observador();
        var customizer = config.sql01HibernateCustomizer(observer);
        Map<String, Object> properties = new HashMap<>();
        customizer.customize(properties);
        var inspector =
                (org.hibernate.resource.jdbc.spi.StatementInspector)
                        properties.get("hibernate.session_factory.statement_inspector");
        assertThat(inspector.inspect("select x from wms.t")).isEqualTo("select x from wms.t");
        assertThatThrownBy(() -> customizer.customize(properties))
                .isInstanceOf(IllegalStateException.class);
        assertThat(properties.get("hibernate.session_factory.statement_inspector"))
                .isSameAs(inspector);
        assertThat(config.sql01ObservationFilter(observer).getOrder())
                .isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1);
    }
}
