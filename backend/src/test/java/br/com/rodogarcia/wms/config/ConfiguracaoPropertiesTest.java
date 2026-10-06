package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfiguracaoPropertiesTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void perfilLocalMantemPersistenciaDesabilitada() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            var env = context.getEnvironment();
                            assertThat(env.getProperty("wms.cadastros.enabled", Boolean.class))
                                    .isFalse();
                            assertThat(env.getProperty("server.address")).isEqualTo("127.0.0.1");
                            assertThat(env.getProperty("spring.autoconfigure.exclude"))
                                    .contains(
                                            "DataSourceAutoConfiguration",
                                            "HibernateJpaAutoConfiguration",
                                            "DataJpaRepositoriesAutoConfiguration");
                        });
    }

    @Test
    void perfilSqlSubstituiExclusoesEResolveVariaveisSemConectar() {
        runner.withPropertyValues(
                        "spring.profiles.active=sqlserver-dev",
                        "WMS_DB_HOST=sql.test.invalid",
                        "WMS_DB_NAME=WMS_DEV",
                        "WMS_DB_USER=usuario-ficticio",
                        "WMS_DB_PASSWORD=senha-ficticia",
                        "WMS_DB_CONFIRMED_TARGET=sql.test.invalid:1433/WMS_DEV",
                        "WMS_DB_CONFIRMED_SERVER=SQL-FICTICIO",
                        "WMS_DB_CERTIFICATE_HOST=CERTIFICADO_FICTICIO",
                        "WMS_DB_TRUST_STORE=certificado-ficticio.p12",
                        "WMS_DB_TRUST_STORE_PASSWORD=integridade-ficticia",
                        "WMS_OIDC_ISSUER=https://identidade.test.invalid",
                        "WMS_OIDC_JWK_SET_URI=https://identidade.test.invalid/jwks",
                        "WMS_OIDC_AUDIENCE=wms-testes")
                .run(
                        context -> {
                            var env = context.getEnvironment();
                            assertThat(env.getProperty("spring.autoconfigure.exclude")).isEmpty();
                            assertThat(env.getProperty("wms.cadastros.enabled", Boolean.class))
                                    .isTrue();
                            assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto"))
                                    .isEqualTo("validate");
                            assertThat(env.getProperty("spring.sql.init.mode")).isEqualTo("never");
                            assertThat(env.getProperty("spring.flyway.enabled", Boolean.class))
                                    .isFalse();
                            var database =
                                    Binder.get(env)
                                            .bind(
                                                    "wms.database",
                                                    Bindable.of(SqlServerProperties.class))
                                            .get();
                            assertThat(database.host()).isEqualTo("sql.test.invalid");
                            assertThat(database.port()).isEqualTo(1433);
                            database.validarAlvo();
                            var jdbc = new SqlServerConfig().configuracao(database, env);
                            assertThat(jdbc.getDataSourceProperties())
                                    .containsEntry("hostNameInCertificate", "CERTIFICADO_FICTICIO")
                                    .containsEntry("trustStore", "certificado-ficticio.p12")
                                    .containsEntry("trustStorePassword", "integridade-ficticia");
                            assertThat(env.getProperty("logging.level.com.zaxxer.hikari"))
                                    .isEqualTo("OFF");
                            var identity =
                                    Binder.get(env)
                                            .bind(
                                                    "wms.identity",
                                                    Bindable.of(IdentidadeProperties.class))
                                            .get();
                            assertThat(identity.audience()).isEqualTo("wms-testes");
                        });
    }
}
