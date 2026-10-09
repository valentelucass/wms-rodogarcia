package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ContextCustomizerFactory;

class D30ContextoLocalGuardTest {
    @Test
    void aceitaContextoNovoDeEmbalagemSomenteNaMemoriaD30Dedicada() {
        var env = seguro("D30EmbalagemFisicoTest");
        assertThat(
                        D30ContextoLocalGuard.conferirConfiguracao("D30EmbalagemFisicoTest", env)
                                .get("URL"))
                .isEqualTo(
                        "jdbc:h2:mem:d30-cedro-embalagem;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms");
    }

    @Test
    void identidadeD29EhFicticiaEspecificaENaoLiberaOutrasSuites() {
        var env = seguro("D29FinanceiroRetomadaTest");
        assertThat(
                        D30ContextoLocalGuard.conferirConfiguracao("D29FinanceiroRetomadaTest", env)
                                .get("identidadeH2Ficticia"))
                .isEqualTo("d29_retomada_local");
        assertThatThrownBy(
                        () ->
                                D30ContextoLocalGuard.conferirConfiguracao(
                                        "D29FinanceiroRetomadaTest",
                                        env.withProperty("spring.datasource.username", "sa")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(
                        () ->
                                D30ContextoLocalGuard.conferirConfiguracao(
                                        "CadastrosIntegrationTest",
                                        seguro("CadastrosIntegrationTest")
                                                .withProperty(
                                                        "spring.datasource.username",
                                                        "d29_retomada_local")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void guardaRegistradaNoSpiRealDosContextosSpring() {
        assertThat(
                        SpringFactoriesLoader.loadFactories(
                                ContextCustomizerFactory.class, getClass().getClassLoader()))
                .anyMatch(D30ContextoLocalGuard.class::isInstance);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "ApiHttpIntegrationTest",
                "CadastrosIntegrationTest",
                "CobrancaIntegrationTest",
                "ContingenciaIntegrationTest",
                "D29FinanceiroRetomadaTest",
                "EstoqueIntegrationTest",
                "ExpedicaoIntegrationTest",
                "FechamentoIntegrationTest",
                "JornadaBackendIntegrationTest",
                "PedidoSaidaIntegrationTest",
                "RecebimentoIntegrationTest",
                "UnidadeLogisticaIntegrationTest"
            })
    void aceitaSomenteConfiguracaoDescobertaDaSuite(String classe) {
        var env = seguro(classe);
        var proof = D30ContextoLocalGuard.conferirConfiguracao(classe, env);
        assertThat(proof.get("sqlServer")).isEqualTo(false);
        assertThat(proof.get("URL")).isEqualTo(D30ContextoLocalGuard.urlEsperada(classe));
    }

    @ParameterizedTest
    @CsvSource({
        "spring.datasource.url,jdbc:sqlserver://alvo-ficticio",
        "spring.datasource.url,jdbc:h2:file:nao-permitido",
        "spring.datasource.url,jdbc:h2:tcp://127.0.0.1/teste",
        "spring.datasource.url,jdbc:h2:mem:outro",
        "spring.flyway.enabled,true",
        "spring.sql.init.mode,always",
        "server.address,0.0.0.0",
        "server.port,8080",
        "spring.datasource.driver-class-name,com.microsoft.sqlserver.jdbc.SQLServerDriver",
        "spring.jpa.properties.hibernate.connection.url,jdbc:outro"
    })
    void recusaSubstituicoesAntesDeInstanciarQualquerBean(String key, String value) {
        assertThatThrownBy(
                        () ->
                                D30ContextoLocalGuard.conferirConfiguracao(
                                        "CadastrosIntegrationTest",
                                        seguro("CadastrosIntegrationTest")
                                                .withProperty(key, value)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("antes dos singletons");
    }

    @ParameterizedTest
    @ValueSource(strings = {"local", "sqlserver-dev", "prod"})
    void recusaPerfisMesmoSemAlvoReal(String perfil) {
        var env = seguro("CadastrosIntegrationTest");
        env.setActiveProfiles(perfil);
        assertThatThrownBy(
                        () ->
                                D30ContextoLocalGuard.conferirConfiguracao(
                                        "CadastrosIntegrationTest", env))
                .isInstanceOf(IllegalStateException.class);
    }

    private static MockEnvironment seguro(String classe) {
        var env =
                new MockEnvironment()
                        .withProperty("server.address", "127.0.0.1")
                        .withProperty("server.port", "0")
                        .withProperty("spring.sql.init.mode", "never")
                        .withProperty("spring.flyway.enabled", "false")
                        .withProperty("spring.jpa.generate-ddl", "false");
        env.setActiveProfiles("test");
        if (D30ContextoLocalGuard.urlEsperada(classe) != null) {
            env.withProperty("spring.datasource.url", D30ContextoLocalGuard.urlEsperada(classe))
                    .withProperty("spring.datasource.driver-class-name", "org.h2.Driver")
                    .withProperty(
                            "spring.datasource.username",
                            "D29FinanceiroRetomadaTest".equals(classe)
                                    ? "d29_retomada_local"
                                    : "sa")
                    .withProperty("spring.datasource.password", "")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "create-drop")
                    .withProperty("spring.autoconfigure.exclude", "")
                    .withProperty("wms.cadastros.enabled", "true")
                    .withProperty("wms.identity.issuer", "https://identidade.test.invalid")
                    .withProperty(
                            "wms.identity.jwk-set-uri", "https://identidade.test.invalid/jwks");
        } else {
            env.withProperty("wms.cadastros.enabled", "false")
                    .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                    .withProperty(
                            "spring.autoconfigure.exclude",
                            "DataSourceAutoConfiguration,HibernateJpaAutoConfiguration,DataJpaRepositoriesAutoConfiguration");
        }
        return env;
    }
}
