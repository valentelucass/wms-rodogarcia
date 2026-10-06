package br.com.rodogarcia.wms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile("sqlserver-dev")
@EnableConfigurationProperties(SqlServerProperties.class)
public class SqlServerConfig {
    @Bean
    DataSource dataSource(SqlServerProperties properties, Environment environment) {
        return new HikariDataSource(configuracao(properties, environment));
    }

    HikariConfig configuracao(SqlServerProperties properties, Environment environment) {
        properties.validarAlvo();
        Map<String, String> jpa =
                Binder.get(environment)
                        .bind("spring.jpa.properties", Bindable.mapOf(String.class, String.class))
                        .orElse(Map.of());
        if (!"validate".equals(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                || environment.getProperty("spring.jpa.generate-ddl", Boolean.class, false)
                || !"never".equals(environment.getProperty("spring.sql.init.mode"))
                || environment.getProperty("spring.flyway.enabled", Boolean.class, false)
                || environment.getProperty("spring.liquibase.enabled", Boolean.class, false)
                || !"validate".equals(jpa.getOrDefault("hibernate.hbm2ddl.auto", "validate"))
                || jpa.keySet().stream().anyMatch(SqlServerConfig::substituiAlvoOuAcao)
                || !"none"
                        .equals(
                                jpa.getOrDefault(
                                        "jakarta.persistence.schema-generation.scripts.action",
                                        "none"))
                || !"none"
                        .equals(
                                jpa.getOrDefault(
                                        "javax.persistence.schema-generation.scripts.action",
                                        "none"))) {
            throw new IllegalStateException(
                    "SQL Server exige validação de esquema e migrations externas à inicialização.");
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(
                "jdbc:sqlserver://"
                        + properties.host()
                        + ":"
                        + properties.port()
                        + ";databaseName="
                        + properties.name()
                        + ";encrypt=true;trustServerCertificate=false");
        config.setUsername(properties.user());
        config.setPassword(properties.password());
        configurarTls(config, environment);
        config.setMaximumPoolSize(5);
        config.setPoolName("wms-dev");
        config.setConnectionInitSql(
                "SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; "
                        + "SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; "
                        + "SET QUOTED_IDENTIFIER ON; SET NUMERIC_ROUNDABORT OFF; "
                        + "IF SERVERPROPERTY('ServerName') IS NULL OR DB_NAME() IS NULL OR "
                        + "CONVERT(nvarchar(128), SERVERPROPERTY('ServerName')) "
                        + "COLLATE Latin1_General_100_BIN2 <> N'"
                        + properties.confirmedServer().replace("'", "''")
                        + "' OR DB_NAME() COLLATE Latin1_General_100_BIN2 <> N'"
                        + properties.name()
                        + "' THROW 50001, 'Identidade SQL Server diferente do alvo WMS confirmado.', 1; "
                        + "IF ISNULL(IS_SRVROLEMEMBER('sysadmin'),1) <> 0 "
                        + "OR ISNULL(IS_MEMBER('db_owner'),1) <> 0 "
                        + "OR ISNULL(IS_MEMBER('db_ddladmin'),1) <> 0 "
                        + "OR ISNULL(HAS_PERMS_BY_NAME(NULL,NULL,'CONTROL SERVER'),1) <> 0 "
                        + "OR ISNULL(HAS_PERMS_BY_NAME(DB_NAME(),'DATABASE','CONTROL'),1) <> 0 "
                        + "OR ISNULL(HAS_PERMS_BY_NAME(DB_NAME(),'DATABASE','CREATE TABLE'),1) <> 0 "
                        + "OR ISNULL(HAS_PERMS_BY_NAME('wms','SCHEMA','ALTER'),1) <> 0 "
                        + "THROW 50002, 'Identidade da aplicacao exige privilegios restritos sem DDL.', 1;");
        return config;
    }

    private static void configurarTls(HikariConfig config, Environment environment) {
        String certificado = environment.getProperty("wms.database.tls.certificate-host", "");
        String trustStore = environment.getProperty("wms.database.tls.trust-store", "");
        String password = environment.getProperty("wms.database.tls.trust-store-password", "");
        if ((!certificado.isEmpty()
                        && (certificado.length() > 253 || !certificado.matches("[A-Za-z0-9._-]+")))
                || trustStore.chars().anyMatch(Character::isISOControl)
                || trustStore.isEmpty() != password.isEmpty()
                || (!trustStore.isEmpty() && trustStore.isBlank())) {
            throw new IllegalStateException("Configuração TLS privada incompleta ou inválida.");
        }
        // Propriedades do driver, sem interpolação na URL ou alteração da confiança global.
        if (!certificado.isEmpty()) {
            config.addDataSourceProperty("hostNameInCertificate", certificado);
        }
        if (!trustStore.isEmpty()) {
            config.addDataSourceProperty("trustStore", trustStore);
            config.addDataSourceProperty("trustStorePassword", password);
            config.addDataSourceProperty("trustStoreType", "PKCS12");
        }
    }

    private static boolean substituiAlvoOuAcao(String chave) {
        return chave.equals("jakarta.persistence.schema-generation.database.action")
                || chave.equals("javax.persistence.schema-generation.database.action")
                || chave.equals("jakarta.persistence.schema-generation-connection")
                || chave.equals("javax.persistence.schema-generation-connection")
                || chave.equals("jakarta.persistence.nonJtaDataSource")
                || chave.equals("jakarta.persistence.jtaDataSource")
                || chave.equals("javax.persistence.nonJtaDataSource")
                || chave.equals("javax.persistence.jtaDataSource")
                || chave.startsWith("jakarta.persistence.jdbc.")
                || chave.startsWith("javax.persistence.jdbc.")
                || chave.startsWith("hibernate.connection.");
    }
}
