package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rodogarcia.wms.dto.AcessoDtos;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.VisaoOperacaoRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.LoginService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:wms-login-d32;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms",
            "wms.auth.enabled=true",
            "wms.auth.origin=http://127.0.0.1:59999",
            "wms.auth.secure-cookie=false"
        })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginIntegrationTest {
    // Somente fixture fictícia H2; não é a senha solicitada para o WMS.
    private static final String TEMPORARIA = "Fixture-temporaria-123!";
    private static final String DEFINITIVA = "Fixture-definitiva-456!";
    @Autowired Environment environment;
    @Autowired JsonMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired LoginService service;
    @Autowired JwtDecoder decoder;
    @Autowired AcessoService acesso;
    @Autowired EntityManager em;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager transactions;
    @MockitoSpyBean VisaoOperacaoRepository visaoRepository;
    private Navegador principal;

    @Test
    void perf01ContaStatementsHttpComSessaoRealH2SemReduzirRevalidacao() throws Exception {
        var tx = new TransactionTemplate(transactions);
        var now = Instant.now();
        var ids =
                tx.execute(
                        status -> {
                            var c = new Cliente("PERFAUTH", "Fictício", "11111111111111", now);
                            var a =
                                    new Armazem(
                                            "PERFAUTH",
                                            "Fictício",
                                            "22222222222222",
                                            "Fictícia",
                                            "SP",
                                            now);
                            em.persist(c);
                            em.persist(a);
                            return List.of(c.getId(), a.getId());
                        });
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        boolean previous = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        var measurements = new ArrayList<Map<String, Object>>();
        var transactionFlags = new AtomicReference<Map<String, Object>>();
        org.mockito.Mockito.doAnswer(
                        invocation -> {
                            var flags = new LinkedHashMap<String, Object>();
                            flags.put(
                                    "springTransactionActive",
                                    TransactionSynchronizationManager.isActualTransactionActive());
                            flags.put(
                                    "springReadOnly",
                                    TransactionSynchronizationManager
                                            .isCurrentTransactionReadOnly());
                            em.unwrap(Session.class)
                                    .doWork(
                                            connection -> {
                                                flags.put(
                                                        "jdbcIsolation",
                                                        connection.getTransactionIsolation());
                                                flags.put(
                                                        "jdbcReadOnlyHint",
                                                        connection.isReadOnly());
                                            });
                            transactionFlags.set(flags);
                            return invocation.callRealMethod();
                        })
                .when(visaoRepository)
                .capacidade(org.mockito.ArgumentMatchers.anyList());
        try {
            int prepared = 0;
            for (int products : List.of(1, 12, 101)) {
                final int from = prepared;
                tx.executeWithoutResult(
                        status -> {
                            var c = em.getReference(Cliente.class, ids.getFirst());
                            for (int i = from; i < products; i++)
                                em.persist(
                                        new Produto(
                                                c,
                                                "AUTH" + i,
                                                "Fictício",
                                                "UN",
                                                TipoQuantidade.CONTAGEM,
                                                0,
                                                false,
                                                false,
                                                null,
                                                now));
                        });
                prepared = products;
                stats.clear();
                long start = System.nanoTime();
                var response =
                        principal.req(
                                "GET",
                                "/api/v1/visao-operacao?clienteId="
                                        + ids.getFirst()
                                        + "&armazemId="
                                        + ids.getLast()
                                        + "&fuso=UTC&tamanho=1",
                                null);
                long elapsed = System.nanoTime() - start;
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(principal.json.path("valorArmazenado").decimalValue())
                        .isEqualByComparingTo("0");
                assertThat(principal.json.path("valorCompleto").asBoolean()).isTrue();
                assertThat(principal.json.path("unidadesArmazenadas").asLong()).isZero();
                var m = new LinkedHashMap<String, Object>();
                m.put("catalogProducts", products);
                m.put("statements", stats.getPrepareStatementCount());
                m.put("entitiesLoaded", stats.getEntityLoadCount());
                m.put("elapsedNanosDiagnosticOnly", elapsed);
                m.put("queryExecutions", stats.getQueryExecutionCount());
                assertThat(transactionFlags.get().get("springTransactionActive")).isEqualTo(true);
                assertThat(transactionFlags.get().get("springReadOnly")).isEqualTo(true);
                assertThat(transactionFlags.get().get("jdbcIsolation"))
                        .isEqualTo(java.sql.Connection.TRANSACTION_SERIALIZABLE);
                m.put("transaction", transactionFlags.get());
                measurements.add(m);
            }
            assertThat(service.jwtAtivo(decoder.decode(principal.token))).isTrue();
            Files.writeString(
                    Path.of(System.getProperty("wms.test.evidencias.dir"), "perf01-auth-http.json"),
                    mapper.writeValueAsString(
                            Map.of(
                                    "context",
                                    "H2 wms-login-d32",
                                    "authEnabled",
                                    true,
                                    "clienteId",
                                    ids.getFirst(),
                                    "armazemId",
                                    ids.getLast(),
                                    "measurements",
                                    measurements,
                                    "beforeComparisonAvailable",
                                    false)),
                    StandardOpenOption.CREATE_NEW);
        } finally {
            stats.setStatisticsEnabled(previous);
            org.mockito.Mockito.reset(visaoRepository);
        }
    }

    @DynamicPropertySource
    static void chaves(DynamicPropertyRegistry r) throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        String key =
                Base64.getEncoder()
                        .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        // Oraculo independente do encoder Spring: mesmo formato do configurador externo.
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        var spec = new PBEKeySpec(TEMPORARIA.toCharArray(), salt, 600000, 256);
        byte[] derived =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                        .generateSecret(spec)
                        .getEncoded();
        spec.clearPassword();
        String hash =
                "{pbkdf2-600k}"
                        + HexFormat.of().formatHex(salt)
                        + HexFormat.of().formatHex(derived);
        r.add("wms.auth.private-key", () -> key);
        r.add("wms.auth.bootstrap-hash", () -> hash);
    }

    @BeforeAll
    void prepararPrincipal() throws Exception {
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:wms-login-d32");
        principal = new Navegador();
        assertThat(principal.login(LoginService.PRINCIPAL_EMAIL, TEMPORARIA).statusCode())
                .isEqualTo(200);
        assertThat(principal.json.path("usuario").path("trocarSenha").asBoolean()).isTrue();
        assertThat(principal.req("GET", "/api/v1/clientes", null).statusCode()).isEqualTo(403);
        assertThat(principal.req("GET", "/api/auth/usuarios", null).statusCode()).isEqualTo(403);
        String antigo = principal.token;
        assertThat(
                        principal
                                .req(
                                        "POST",
                                        "/api/auth/senha",
                                        Map.of("senhaAtual", TEMPORARIA, "novaSenha", DEFINITIVA))
                                .statusCode())
                .isEqualTo(204);
        principal.token = antigo;
        assertThat(principal.req("GET", "/api/auth/eu", null).statusCode()).isEqualTo(401);
        assertThat(principal.login(LoginService.PRINCIPAL_EMAIL, DEFINITIVA).statusCode())
                .isEqualTo(200);
        assertThat(principal.json.path("usuario").path("administrador").asBoolean()).isTrue();
    }

    @Test
    void cicloUsuarioAdministradorDelegadoRedefinicaoEProtecaoPrincipal() throws Exception {
        String email = UUID.randomUUID() + "@test.invalid";
        JsonNode user = criar(email, true);
        var delegado = new Navegador();
        assertThat(delegado.login(email, TEMPORARIA).statusCode()).isEqualTo(200);
        assertThat(delegado.req("GET", "/api/auth/usuarios", null).statusCode()).isEqualTo(403);
        assertThat(
                        delegado.req(
                                        "POST",
                                        "/api/auth/senha",
                                        Map.of("senhaAtual", TEMPORARIA, "novaSenha", DEFINITIVA))
                                .statusCode())
                .isEqualTo(204);
        assertThat(delegado.login(email, DEFINITIVA).statusCode()).isEqualTo(200);
        assertThat(delegado.req("GET", "/api/auth/usuarios", null).statusCode()).isEqualTo(200);
        assertThat(
                        delegado.req(
                                        "POST",
                                        "/api/auth/usuarios",
                                        cadastro(UUID.randomUUID() + "@test.invalid", false))
                                .statusCode())
                .isEqualTo(201);
        assertThat(
                        delegado.req(
                                        "POST",
                                        "/api/auth/usuarios/"
                                                + LoginService.PRINCIPAL_ID
                                                + "/senha",
                                        Map.of("senhaTemporaria", TEMPORARIA, "versao", 0))
                                .statusCode())
                .isEqualTo(403);
        assertThat(
                        delegado.req(
                                        "PUT",
                                        "/api/auth/usuarios/" + LoginService.PRINCIPAL_ID,
                                        Map.of(
                                                "nome",
                                                "Alterado",
                                                "perfil",
                                                "OPERACAO",
                                                "administrador",
                                                false,
                                                "ativo",
                                                false,
                                                "clientes",
                                                List.of(),
                                                "armazens",
                                                List.of(),
                                                "versao",
                                                0))
                                .statusCode())
                .isEqualTo(403);
        assertThat(
                        delegado.req(
                                        "DELETE",
                                        "/api/auth/usuarios/" + LoginService.PRINCIPAL_ID,
                                        null)
                                .statusCode())
                .isEqualTo(405);
        String id = user.path("id").asString();
        principal.req("GET", "/api/auth/usuarios", null);
        long versao =
                jdbc.queryForObject(
                        "select versao from wms.usuario_acesso where id = ?", Long.class, id);
        assertThat(
                        principal
                                .req(
                                        "POST",
                                        "/api/auth/usuarios/" + id + "/senha",
                                        Map.of("senhaTemporaria", TEMPORARIA, "versao", versao))
                                .statusCode())
                .isEqualTo(204);
        assertThat(delegado.req("GET", "/api/auth/eu", null).statusCode()).isEqualTo(401);
        delegado.token = null;
        assertThat(delegado.req("POST", "/api/auth/renovar", null).statusCode()).isEqualTo(401);
        assertThat(delegado.login(email, TEMPORARIA).statusCode()).isEqualTo(200);
        assertThat(delegado.json.path("usuario").path("trocarSenha").asBoolean()).isTrue();
        assertThat(delegado.req("GET", "/api/v1/clientes", null).statusCode()).isEqualTo(403);
    }

    @Test
    void csrfOrigemCookieELogoutRevogamJwt() throws Exception {
        var navegador = new Navegador();
        var raw =
                HttpRequest.newBuilder(navegador.uri("/api/auth/entrar"))
                        .header("Content-Type", "application/json")
                        .header("Origin", "http://127.0.0.1:59999")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        mapper.writeValueAsString(
                                                Map.of(
                                                        "email",
                                                        LoginService.PRINCIPAL_EMAIL,
                                                        "senha",
                                                        DEFINITIVA))))
                        .build();
        assertThat(navegador.http.send(raw, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isEqualTo(403);
        var login = navegador.login(LoginService.PRINCIPAL_EMAIL, DEFINITIVA);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.headers().allValues("Set-Cookie").toString())
                .contains("HttpOnly", "SameSite=Strict", "Path=/api/auth");
        assertThat(login.body()).doesNotContain("senhaHash", "senha_hash", "renovacao", DEFINITIVA);
        var foreign =
                HttpRequest.newBuilder(navegador.uri("/api/auth/sair"))
                        .header("Origin", "https://outro.invalid")
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
        assertThat(navegador.http.send(foreign, HttpResponse.BodyHandlers.ofString()).statusCode())
                .isEqualTo(403);
        assertThat(navegador.req("GET", "/api/v1/clientes", null).statusCode()).isEqualTo(200);
        assertThat(navegador.req("POST", "/api/auth/sair", null).statusCode()).isEqualTo(204);
        assertThat(navegador.req("GET", "/api/v1/clientes", null).statusCode()).isEqualTo(401);
    }

    @Test
    void rotacaoDetectaReusoERevogaFamilia() throws Exception {
        var navegador = new Navegador();
        assertThat(navegador.login(LoginService.PRINCIPAL_EMAIL, DEFINITIVA).statusCode())
                .isEqualTo(200);
        var antigo =
                navegador.cookies.getCookieStore().getCookies().stream()
                        .filter(c -> c.getName().equals("WMS_REFRESH"))
                        .findFirst()
                        .orElseThrow();
        String tokenAnterior = antigo.getValue();
        navegador.token = null;
        assertThat(navegador.req("POST", "/api/auth/renovar", null).statusCode()).isEqualTo(200);
        String accessNovo = navegador.json.path("accessToken").asString();
        assertThat(
                        navegador.cookies.getCookieStore().getCookies().stream()
                                .filter(c -> c.getName().equals("WMS_REFRESH"))
                                .findFirst()
                                .orElseThrow()
                                .getValue())
                .isNotEqualTo(tokenAnterior);
        navegador.cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals("WMS_REFRESH"))
                .forEach(c -> c.setValue(tokenAnterior));
        assertThat(navegador.req("POST", "/api/auth/renovar", null).statusCode()).isEqualTo(401);
        navegador.token = accessNovo;
        assertThat(navegador.req("GET", "/api/auth/eu", null).statusCode()).isEqualTo(401);
    }

    @Test
    void usuarioComumNaoAdministraEPrimeiroAcessoNaoRecebeDadosOperacionais() throws Exception {
        String email = UUID.randomUUID() + "@test.invalid";
        criar(email, false);
        var n = new Navegador();
        n.login(email, TEMPORARIA);
        assertThat(
                        n.req("POST", "/api/auth/usuarios", cadastro("negado@test.invalid", true))
                                .statusCode())
                .isEqualTo(403);
        n.req("POST", "/api/auth/senha", Map.of("senhaAtual", TEMPORARIA, "novaSenha", DEFINITIVA));
        n.login(email, DEFINITIVA);
        assertThat(n.req("GET", "/api/auth/usuarios", null).statusCode()).isEqualTo(403);
        assertThat(
                        n.req("POST", "/api/auth/usuarios", cadastro("negado@test.invalid", true))
                                .statusCode())
                .isEqualTo(403);
        assertThatThrownBy(
                        () ->
                                service.criar(
                                        new AcessoDtos.CriarUsuario(
                                                "Intruso",
                                                "direto@test.invalid",
                                                TEMPORARIA,
                                                "GESTOR",
                                                true,
                                                List.of(),
                                                List.of())))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void bootstrapIdempotenteNaoRestauraSenhaENenhumHashVazaNaLista() throws Exception {
        String antes =
                jdbc.queryForObject(
                        "select senha_hash from wms.usuario_acesso where id = ?",
                        String.class,
                        LoginService.PRINCIPAL_ID);
        service.iniciarPrincipal("material-invalido-ignorado-apos-criacao");
        assertThat(
                        jdbc.queryForObject(
                                "select senha_hash from wms.usuario_acesso where id = ?",
                                String.class,
                                LoginService.PRINCIPAL_ID))
                .isEqualTo(antes)
                .doesNotContain(DEFINITIVA, TEMPORARIA);
        assertThat(principal.req("GET", "/api/auth/usuarios", null).body())
                .doesNotContain("senhaHash", "senha_hash", "pbkdf2", DEFINITIVA, TEMPORARIA);
        assertThat(
                        principal
                                .req(
                                        "POST",
                                        "/api/auth/usuarios",
                                        cadastro(LoginService.PRINCIPAL_EMAIL.toUpperCase(), false))
                                .statusCode())
                .isEqualTo(409);
    }

    @Test
    void principalConsultaVisaoGeralSemVinculosExplicitosDeClienteOuArmazem() throws Exception {
        // Mesmo login próprio utilizado pelo frontend, exclusivamente na fixture H2.
        var navegador = principal;
        assertThat(navegador.req("GET", "/api/auth/eu", null).statusCode()).isEqualTo(200);
        assertThat(navegador.json.path("perfil").asString()).isEqualTo("GESTOR");
        String codigo = "VISAO-" + UUID.randomUUID().toString().substring(0, 8);
        assertThat(
                        navegador
                                .req(
                                        "POST",
                                        "/api/v1/clientes",
                                        Map.of(
                                                "codigo",
                                                codigo,
                                                "nome",
                                                "Cliente da visão fictícia",
                                                "documentoFiscal",
                                                codigo))
                                .statusCode())
                .isEqualTo(201);
        long clienteId = navegador.json.path("id").asLong();
        assertThat(
                        navegador
                                .req(
                                        "POST",
                                        "/api/v1/armazens",
                                        Map.of(
                                                "codigo",
                                                codigo,
                                                "nome",
                                                "Armazém da visão fictícia",
                                                "documentoFiscal",
                                                codigo,
                                                "cidade",
                                                "Campinas",
                                                "uf",
                                                "SP"))
                                .statusCode())
                .isEqualTo(201);
        long armazemId = navegador.json.path("id").asLong();
        String consulta = "/api/v1/visao-operacao?fuso=America/Sao_Paulo";
        for (String contexto :
                List.of(
                        "",
                        "&armazemId=" + armazemId,
                        "&clienteId=" + clienteId + "&armazemId=" + armazemId)) {
            assertThat(navegador.req("GET", consulta + contexto, null).statusCode()).isEqualTo(200);
            assertThat(navegador.json.path("financeiroPermitido").asBoolean()).isTrue();
        }
    }

    private JsonNode criar(String email, boolean admin) throws Exception {
        assertThat(principal.req("POST", "/api/auth/usuarios", cadastro(email, admin)).statusCode())
                .isEqualTo(201);
        return principal.json;
    }

    @Test
    void bloqueioPorFalhasPersisteSemExporExistenciaDaConta() throws Exception {
        String email = UUID.randomUUID() + "@test.invalid";
        JsonNode criado = criar(email, false);
        var n = new Navegador();
        for (int i = 0; i < 5; i++)
            assertThat(n.login(email, "errada-fixture-123").statusCode()).isEqualTo(401);
        String recusado = n.json.path("detail").asString();
        assertThat(n.login(email, TEMPORARIA).statusCode()).isEqualTo(401);
        assertThat(n.json.path("detail").asString()).isEqualTo(recusado);
        assertThat(
                        jdbc.queryForObject(
                                "select falhas from wms.usuario_acesso where id = ?",
                                Integer.class,
                                criado.path("id").asString()))
                .isEqualTo(5);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.sessao_acesso where usuario_id = ?",
                                Integer.class,
                                criado.path("id").asString()))
                .isZero();
    }

    @Test
    void alteracaoDePermissoesRevogaAcessoEConflitoNaoSobrescreveCadastro() throws Exception {
        String email = UUID.randomUUID() + "@test.invalid";
        String id = criar(email, true).path("id").asString();
        var n = new Navegador();
        n.login(email, TEMPORARIA);
        n.req("POST", "/api/auth/senha", Map.of("senhaAtual", TEMPORARIA, "novaSenha", DEFINITIVA));
        n.login(email, DEFINITIVA);
        long versao =
                jdbc.queryForObject(
                        "select versao from wms.usuario_acesso where id = ?", Long.class, id);
        var alteracao =
                Map.of(
                        "nome",
                        "Sem administração",
                        "perfil",
                        "OPERACAO",
                        "administrador",
                        false,
                        "ativo",
                        true,
                        "clientes",
                        List.of(),
                        "armazens",
                        List.of(),
                        "versao",
                        versao);
        assertThat(principal.req("PUT", "/api/auth/usuarios/" + id, alteracao).statusCode())
                .isEqualTo(200);
        assertThat(n.req("GET", "/api/auth/usuarios", null).statusCode()).isEqualTo(401);
        assertThat(principal.req("PUT", "/api/auth/usuarios/" + id, alteracao).statusCode())
                .isEqualTo(409);
    }

    @Test
    void renovacaoConcorrenteNaoCriaDuasSessoesValidas() throws Exception {
        var n = new Navegador();
        assertThat(n.login(LoginService.PRINCIPAL_EMAIL, DEFINITIVA).statusCode()).isEqualTo(200);
        String refresh =
                n.cookies.getCookieStore().getCookies().stream()
                        .filter(c -> c.getName().equals("WMS_REFRESH"))
                        .findFirst()
                        .orElseThrow()
                        .getValue();
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> acao =
                    () -> {
                        start.await();
                        try {
                            return service.renovar(refresh).resposta().accessToken();
                        } catch (br.com.rodogarcia.wms.exceptions.FalhaLoginException ex) {
                            return "recusado";
                        }
                    };
            var a = pool.submit(acao);
            var b = pool.submit(acao);
            start.countDown();
            var resultados =
                    List.of(
                            a.get(20, java.util.concurrent.TimeUnit.SECONDS),
                            b.get(20, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(resultados.stream().filter("recusado"::equals).count()).isEqualTo(1);
            n.token =
                    resultados.stream()
                            .filter(v -> !v.equals("recusado"))
                            .findFirst()
                            .orElseThrow();
            assertThat(n.req("GET", "/api/auth/eu", null).statusCode()).isEqualTo(401);
        } finally {
            pool.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"sessao_expirada", "usuario_inativo", "versao_revogada"})
    void qualConf01RevalidaSessaoNoHttpENoServicoComJwtAindaValido(String estado) throws Exception {
        String email = UUID.randomUUID() + "@test.invalid";
        String id = criar(email, false).path("id").asString();
        var n = new Navegador();
        // Emissão pelo serviço real H2: não acrescenta tentativas ao ensaio existente de rate
        // limit.
        n.token =
                service.entrar(new AcessoDtos.Login(email, TEMPORARIA), null)
                        .resposta()
                        .accessToken();
        assertThat(
                        n.req(
                                        "POST",
                                        "/api/auth/senha",
                                        Map.of("senhaAtual", TEMPORARIA, "novaSenha", DEFINITIVA))
                                .statusCode())
                .isEqualTo(204);
        var entrada = service.entrar(new AcessoDtos.Login(email, DEFINITIVA), null);
        n.token = entrada.resposta().accessToken();
        var cookie = new HttpCookie("WMS_REFRESH", entrada.renovacao());
        cookie.setVersion(0);
        cookie.setPath("/api/auth");
        n.cookies.getCookieStore().add(n.uri("/api/auth"), cookie);
        var jwt = decoder.decode(n.token);
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(service.jwtAtivo(jwt)).isTrue();
        assertThat(n.req("GET", "/api/v1/clientes", null).statusCode()).isEqualTo(200);
        long sessoes =
                jdbc.queryForObject(
                        "select count(*) from wms.sessao_acesso where usuario_id=?",
                        Long.class,
                        id);
        long renovacoes =
                jdbc.queryForObject(
                        "select count(*) from wms.renovacao_acesso r join wms.sessao_acesso s on s.id=r.sessao_id where s.usuario_id=?",
                        Long.class,
                        id);
        int alterados =
                switch (estado) {
                    case "sessao_expirada" ->
                            jdbc.update(
                                    "update wms.sessao_acesso set expira=? where id=?",
                                    java.sql.Timestamp.from(Instant.now().minusSeconds(60)),
                                    jwt.getClaimAsString("sid"));
                    case "usuario_inativo" ->
                            jdbc.update("update wms.usuario_acesso set ativo=false where id=?", id);
                    case "versao_revogada" ->
                            jdbc.update(
                                    "update wms.usuario_acesso set versao_tokens=versao_tokens+1 where id=?",
                                    id);
                    default -> throw new AssertionError(estado);
                };
        assertThat(alterados).isEqualTo(1);
        assertThat(service.jwtAtivo(jwt)).isFalse();
        var anterior = SecurityContextHolder.getContext();
        try {
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
            SecurityContextHolder.setContext(contexto);
            assertThatThrownBy(acesso::usuario).isInstanceOf(AccessDeniedException.class);
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
        assertThat(n.req("GET", "/api/auth/eu", null).statusCode()).isEqualTo(401);
        assertThat(n.req("GET", "/api/v1/clientes", null).statusCode()).isEqualTo(401);
        assertThat(n.req("GET", "/api/v1/visao-operacao?fuso=UTC", null).statusCode())
                .isEqualTo(401);
        assertThat(n.req("GET", "/api/v1/dashboard?fuso=UTC", null).statusCode()).isEqualTo(401);
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        n.token = null;
        assertThat(n.req("POST", "/api/auth/renovar", null).statusCode()).isEqualTo(401);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.sessao_acesso where usuario_id=?",
                                Long.class,
                                id))
                .isEqualTo(sessoes);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from wms.renovacao_acesso r join wms.sessao_acesso s on s.id=r.sessao_id where s.usuario_id=?",
                                Long.class,
                                id))
                .isEqualTo(renovacoes);
    }

    private Map<String, Object> cadastro(String email, boolean admin) {
        return Map.of(
                "nome",
                "Usuário de teste",
                "email",
                email,
                "senhaTemporaria",
                TEMPORARIA,
                "perfil",
                "GESTOR",
                "administrador",
                admin,
                "clientes",
                List.of(),
                "armazens",
                List.of());
    }

    private class Navegador {
        final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        final HttpClient http =
                HttpClient.newBuilder()
                        .cookieHandler(cookies)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
        String token;
        JsonNode json;

        URI uri(String path) {
            return URI.create(
                    "http://127.0.0.1:" + environment.getProperty("local.server.port") + path);
        }

        HttpResponse<String> login(String email, String senha) throws Exception {
            token = null;
            var res = req("POST", "/api/auth/entrar", Map.of("email", email, "senha", senha));
            if (res.statusCode() == 200) token = json.path("accessToken").asString();
            return res;
        }

        HttpResponse<String> req(String method, String path, Object body) throws Exception {
            var b =
                    HttpRequest.newBuilder(uri(path))
                            .timeout(Duration.ofSeconds(30))
                            .header("Accept", "application/json");
            if (token != null) b.header("Authorization", "Bearer " + token);
            if (!method.equals("GET")) {
                var csrf =
                        http.send(
                                HttpRequest.newBuilder(uri("/api/auth/csrf")).GET().build(),
                                HttpResponse.BodyHandlers.ofString());
                var c = mapper.readTree(csrf.body());
                b.header(c.path("header").asString(), c.path("token").asString())
                        .header("Origin", "http://127.0.0.1:59999")
                        .header("Content-Type", "application/json");
            }
            var res =
                    http.send(
                            b.method(
                                            method,
                                            body == null
                                                    ? HttpRequest.BodyPublishers.noBody()
                                                    : HttpRequest.BodyPublishers.ofString(
                                                            mapper.writeValueAsString(body)))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            json = res.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(res.body());
            return res;
        }
    }
}
