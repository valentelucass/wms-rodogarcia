package br.com.rodogarcia.wms.config;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import tools.jackson.databind.json.JsonMapper;

/** Guarda somente de testes D30: confere ambiente antes de instanciar DataSource/servicos. */
public class D30ContextoLocalGuard implements ContextCustomizerFactory {
    private static final Map<String, String> BANCOS =
            Map.ofEntries(
                    Map.entry("CadastrosIntegrationTest", "wms-cadastros"),
                    Map.entry("CobrancaIntegrationTest", "wms-cobranca"),
                    Map.entry("ContingenciaIntegrationTest", "wms-be14"),
                    Map.entry("D29FinanceiroRetomadaTest", "wms-d29-retomada"),
                    Map.entry("D30EmbalagemFisicoTest", "d30-cedro-embalagem"),
                    Map.entry("EstoqueIntegrationTest", "wms-estoque"),
                    Map.entry("ExpedicaoIntegrationTest", "wms-expedicao"),
                    Map.entry("FechamentoIntegrationTest", "wms-fechamento"),
                    Map.entry("JornadaBackendIntegrationTest", "wms-jornada"),
                    Map.entry("PedidoSaidaIntegrationTest", "wms-saida"),
                    Map.entry("RecebimentoIntegrationTest", "wms-recebimento"),
                    Map.entry("UnidadeLogisticaIntegrationTest", "wms-unidades"));

    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        if (!"D30".equals(System.getProperty("wms.test.local.guard"))
                || !testClass.isAnnotationPresent(SpringBootTest.class)) {
            return null;
        }
        return new Guarda(testClass.getSimpleName());
    }

    static String urlEsperada(String classe) {
        String banco = BANCOS.get(classe);
        return banco == null
                ? null
                : "jdbc:h2:mem:"
                        + banco
                        + ";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms";
    }

    public static boolean urlCapturaLocalPermitida(String url) {
        return BANCOS.values().stream().anyMatch(nome -> ("jdbc:h2:mem:" + nome).equals(url));
    }

    static Map<String, Object> conferirConfiguracao(String classe, Environment env) {
        boolean header = "D30JwtHttpHeaderTest".equals(classe);
        exigir(
                "ApiHttpIntegrationTest".equals(classe) || header || BANCOS.containsKey(classe),
                "suite");
        exigir(Arrays.equals(env.getActiveProfiles(), new String[] {"test"}), "perfil");
        exigir("127.0.0.1".equals(env.getProperty("server.address")), "HTTP endereco");
        exigir("0".equals(env.getProperty("server.port")), "HTTP porta efemera");
        exigir("never".equals(env.getProperty("spring.sql.init.mode")), "scripts");
        exigir(!env.getProperty("spring.flyway.enabled", Boolean.class, true), "Flyway");
        exigir(!env.getProperty("spring.liquibase.enabled", Boolean.class, false), "Liquibase");
        exigir(!env.getProperty("spring.jpa.generate-ddl", Boolean.class, true), "generateDDL");
        for (String key :
                List.of(
                        "spring.datasource.jndi-name",
                        "spring.datasource.hikari.jdbc-url",
                        "spring.datasource.hikari.data-source-class-name",
                        "spring.jpa.properties.hibernate.connection.url",
                        "spring.jpa.properties.hibernate.hbm2ddl.auto",
                        "spring.jpa.properties.jakarta.persistence.jdbc.url",
                        "spring.jpa.properties.jakarta.persistence.nonJtaDataSource",
                        "spring.jpa.properties.jakarta.persistence.schema-generation.database.action",
                        "spring.jpa.properties.javax.persistence.schema-generation.database.action")) {
            exigir(env.getProperty(key) == null, "sobreposicao proibida");
        }
        String url = env.getProperty("spring.datasource.url");
        boolean h2 = BANCOS.containsKey(classe);
        if (h2) {
            exigir(urlEsperada(classe).equals(url), "URL H2 isolada");
            exigir(
                    "org.h2.Driver".equals(env.getProperty("spring.datasource.driver-class-name")),
                    "driver");
            exigir(
                    ("D29FinanceiroRetomadaTest".equals(classe) ? "d29_retomada_local" : "sa")
                            .equals(env.getProperty("spring.datasource.username")),
                    "identidade ficticia H2");
            exigir("".equals(env.getProperty("spring.datasource.password")), "H2 sem segredo");
            exigir(
                    "create-drop".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")),
                    "schema H2");
            exigir(
                    "".equals(env.getProperty("spring.autoconfigure.exclude")),
                    "H2 autoconfiguracao");
            exigir(
                    env.getProperty("wms.cadastros.enabled", Boolean.class, false),
                    "persistencia H2");
            exigir(
                    "https://identidade.test.invalid"
                            .equals(env.getProperty("wms.identity.issuer")),
                    "issuer ficticio");
            exigir(
                    "https://identidade.test.invalid/jwks"
                            .equals(env.getProperty("wms.identity.jwk-set-uri")),
                    "JWKS ficticio");
        } else {
            exigir(url == null, "ApiHttp sem URL");
            exigir(
                    env.getProperty("wms.cadastros.enabled", Boolean.class, !header) == header,
                    "configuracao sem persistencia");
            String exclusions = env.getProperty("spring.autoconfigure.exclude", "");
            exigir(
                    exclusions.contains("DataSourceAutoConfiguration")
                            && exclusions.contains("HibernateJpaAutoConfiguration")
                            && exclusions.contains("DataJpaRepositoriesAutoConfiguration"),
                    "ApiHttp exclusoes");
            exigir(
                    "validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")),
                    "ApiHttp sem DDL");
        }
        var proof = new LinkedHashMap<String, Object>();
        proof.put("classe", classe);
        proof.put("perfis", Arrays.asList(env.getActiveProfiles()));
        proof.put("serverAddress", "127.0.0.1");
        proof.put("serverPort", 0);
        proof.put("URL", url);
        proof.put("driver", h2 ? "org.h2.Driver" : null);
        proof.put(
                "identidadeH2Ficticia",
                h2
                        ? ("D29FinanceiroRetomadaTest".equals(classe) ? "d29_retomada_local" : "sa")
                        : null);
        proof.put("banco", h2 ? "H2_MEM_EFEMERO_FICTICIO" : "SEM_DATASOURCE");
        proof.put("schema", h2 ? "create-drop somente H2 memory" : "nenhum");
        proof.put("flyway", false);
        proof.put("sqlServer", false);
        return proof;
    }

    private static void exigir(boolean condition, String limit) {
        if (!condition) {
            // Nunca incluir valores recebidos: podem conter configuracao externa/segredo.
            throw new IllegalStateException(
                    "Guarda D30 recusou " + limit + " antes dos singletons.");
        }
    }

    private record Guarda(String classe) implements ContextCustomizer {
        @Override
        public void customizeContext(
                ConfigurableApplicationContext context, MergedContextConfiguration merged) {
            context.addBeanFactoryPostProcessor(
                    factory -> {
                        var proof = conferirConfiguracao(classe, context.getEnvironment());
                        exigir(
                                factory.getBeanNamesForType(SqlServerConfig.class, false, false)
                                                .length
                                        == 0,
                                "config SQLServer");
                        if (BANCOS.containsKey(classe) || "D30JwtHttpHeaderTest".equals(classe)) {
                            exigir(
                                    factory.containsBeanDefinition("jwtDecoder"),
                                    "decoder ficticio");
                            String owner =
                                    factory.getBeanDefinition("jwtDecoder").getFactoryBeanName();
                            exigir(
                                    owner != null
                                            && factory.containsBeanDefinition(owner)
                                            && IdentidadeTesteConfig.class
                                                    .getName()
                                                    .equals(
                                                            factory.getBeanDefinition(owner)
                                                                    .getBeanClassName()),
                                    "decoder local sem JWKS externo");
                        }
                        if (!BANCOS.containsKey(classe)) {
                            exigir(
                                    factory.getBeanNamesForType(DataSource.class, false, false)
                                                    .length
                                            == 0,
                                    "ApiHttp DataSource");
                            exigir(
                                    factory.getBeanNamesForType(
                                                            EntityManagerFactory.class,
                                                            false,
                                                            false)
                                                    .length
                                            == 0,
                                    "ApiHttp JPA");
                        }
                        proof.put("etapa", "CONFIG_EFETIVA_ANTES_SINGLETONS");
                        String leituras = System.getProperty("wms.test.leituras.sem-efeito");
                        exigir(leituras == null || "D30_FINAL".equals(leituras), "modo GET local");
                        if (BANCOS.containsKey(classe) && leituras != null) {
                            exigir(
                                    factory instanceof BeanDefinitionRegistry,
                                    "registro observador GET");
                            var definition =
                                    BeanDefinitionBuilder.rootBeanDefinition(
                                                    br.com.rodogarcia.wms.D30GetSemEfeitoFilter
                                                            .class)
                                            .addConstructorArgValue(classe)
                                            .setAutowireMode(
                                                    AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
                                            .getBeanDefinition();
                            ((BeanDefinitionRegistry) factory)
                                    .registerBeanDefinition("d30GetSemEfeitoFilter", definition);
                            proof.put(
                                    "observacaoGET",
                                    "D30_FINAL;64tabelas;temporaisTipados;semNovasChamadas");
                        }
                        salvar(classe + "-antes", proof);
                    });
            context.addApplicationListener(
                    event -> {
                        if (event instanceof ContextRefreshedEvent refreshed
                                && refreshed.getApplicationContext() == context) {
                            var proof = conferirConfiguracao(classe, context.getEnvironment());
                            var sources = context.getBeansOfType(DataSource.class);
                            exigir(
                                    sources.size() == (BANCOS.containsKey(classe) ? 1 : 0),
                                    "quantidade DataSources");
                            if (!sources.isEmpty()) {
                                try (var conn =
                                        sources.values().iterator().next().getConnection()) {
                                    String actual = conn.getMetaData().getURL();
                                    exigir(
                                            actual.equals("jdbc:h2:mem:" + BANCOS.get(classe)),
                                            "metadata H2");
                                    proof.put("metadataURL", actual);
                                    proof.put(
                                            "produto", conn.getMetaData().getDatabaseProductName());
                                } catch (java.sql.SQLException e) {
                                    throw new IllegalStateException(
                                            "Guarda D30: metadados locais indisponiveis.");
                                }
                            }
                            proof.put("dataSources", sources.size());
                            int jpa = context.getBeansOfType(EntityManagerFactory.class).size();
                            exigir(
                                    jpa == (BANCOS.containsKey(classe) ? 1 : 0),
                                    "quantidade contextos JPA");
                            proof.put("entityManagerFactories", jpa);
                            if (jpa == 1) {
                                br.com.rodogarcia.wms.D30IdentidadeJpaObservador.instalar(
                                        context.getBean(EntityManagerFactory.class), classe);
                            }
                            proof.put("etapa", "CONTEXTO_EFETIVO_ANTES_CASOS");
                            salvar(classe + "-depois", proof);
                        }
                    });
        }
    }

    private static void salvar(String nome, Map<String, Object> proof) {
        String dir = System.getProperty("wms.test.evidencias.dir");
        exigir(dir != null, "destino evidencia propria");
        try {
            Path folder = Path.of(dir).toAbsolutePath().normalize();
            exigir(folder.toString().contains("d30-cedro-"), "destino D30");
            Files.createDirectories(folder);
            Files.writeString(
                    folder.resolve("d30-cedro-contexto-" + nome + ".json"),
                    JsonMapper.builder()
                            .build()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(proof),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Guarda D30: evidencia local nao preservavel.");
        }
    }
}
