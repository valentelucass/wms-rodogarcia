package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

class SqlServerConfigTest {
    @Test
    void recusaAlvoNaoConferidoSemAbrirConexao() {
        var properties =
                new SqlServerProperties(
                        "sql.test.invalid",
                        1433,
                        "WMS_DEV",
                        "usuario-ficticio",
                        "senha-ficticia",
                        "outro",
                        "SQL-FICTICIO");
        assertThatThrownBy(properties::validarAlvo).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void representacaoDaConfiguracaoNaoIncluiCredenciais() {
        var properties = propriedades();
        assertThat(properties.toString()).doesNotContain("usuario-ficticio", "senha-ficticia");
    }

    @Test
    void impedeAlteracaoAutomaticaDeEsquemaAntesDeAbrirConexao() {
        var environment =
                new MockEnvironment()
                        .withProperty("spring.jpa.hibernate.ddl-auto", "update")
                        .withProperty("spring.sql.init.mode", "never");
        assertThatThrownBy(() -> new SqlServerConfig().dataSource(propriedades(), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migrations externas");
    }

    @Test
    void impedeInicializacaoDeScriptsAntesDeAbrirConexao() {
        var environment =
                new MockEnvironment()
                        .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                        .withProperty("spring.sql.init.mode", "always");
        assertThatThrownBy(() -> new SqlServerConfig().dataSource(propriedades(), environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migrations externas");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "spring.jpa.properties.hibernate.hbm2ddl.auto=update",
                "spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create",
                "spring.jpa.properties[jakarta.persistence.schema-generation.database.action]=none",
                "spring.jpa.properties.javax.persistence.schema-generation.database.action=drop-and-create",
                "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create",
                "spring.jpa.properties.hibernate.connection.url=jdbc:outro",
                "spring.jpa.properties.jakarta.persistence.jdbc.url=jdbc:outro",
                "spring.jpa.properties.jakarta.persistence.nonJtaDataSource=outro",
                "spring.jpa.properties.jakarta.persistence.schema-generation-connection=outra",
                "spring.jpa.generate-ddl=true",
                "spring.flyway.enabled=true",
                "spring.liquibase.enabled=true"
            })
    void recusaSobreposicoesDeEsquemaOuAlvoAntesDeConectar(String propriedade) {
        String[] partes = propriedade.split("=", 2);
        var env = ambienteSeguro().withProperty(partes[0], partes[1]);
        assertThatThrownBy(() -> new SqlServerConfig().dataSource(propriedades(), env))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("migrations externas");
    }

    @Test
    void configuraTlsEIdentidadeEmCadaConexaoSemAbrirPool() {
        var config = new SqlServerConfig().configuracao(propriedades(), ambienteSeguro());
        assertThat(config.getJdbcUrl()).endsWith(";encrypt=true;trustServerCertificate=false");
        assertThat(config.getJdbcUrl()).doesNotContain("usuario-ficticio", "senha-ficticia");
        assertThat(config.getConnectionInitSql())
                .contains(
                        "SET ANSI_NULLS ON",
                        "SET ANSI_PADDING ON",
                        "SET ANSI_WARNINGS ON",
                        "SET ARITHABORT ON",
                        "SET CONCAT_NULL_YIELDS_NULL ON",
                        "SET QUOTED_IDENTIFIER ON",
                        "SET NUMERIC_ROUNDABORT OFF",
                        "SERVERPROPERTY('ServerName')",
                        "SERVERPROPERTY('ServerName') IS NULL",
                        "DB_NAME() IS NULL",
                        "DB_NAME()",
                        "THROW 50001")
                .doesNotContain("usuario-ficticio", "senha-ficticia");
    }

    @Test
    void escapaNomeConfirmadoSemPermitirSqlAdicional() {
        var p =
                new SqlServerProperties(
                        "sql.test.invalid",
                        1433,
                        "WMS_DEV",
                        "usuario-ficticio",
                        "senha-ficticia",
                        "sql.test.invalid:1433/WMS_DEV",
                        "SQL'FICTICIO");
        assertThat(new SqlServerConfig().configuracao(p, ambienteSeguro()).getConnectionInitSql())
                .contains("N'SQL''FICTICIO'");
    }

    @Test
    void recusaParametrosMalformadosMesmoSemBinder() {
        for (var p :
                java.util.List.of(
                        new SqlServerProperties(
                                "host;encrypt=false", 1433, "WMS_DEV", "u", "s", "outro", "SQL"),
                        new SqlServerProperties(
                                "sql.test.invalid", 0, "WMS_DEV", "u", "s", "outro", "SQL"),
                        new SqlServerProperties(
                                "sql.test.invalid",
                                1433,
                                "WMS_DEV;outro",
                                "u",
                                "s",
                                "outro",
                                "SQL"),
                        new SqlServerProperties(
                                "sql.test.invalid",
                                1433,
                                "WMS_DEV",
                                "u",
                                "s",
                                "sql.test.invalid:1433/WMS_DEV",
                                ""))) {
            assertThatThrownBy(p::validarAlvo).isInstanceOf(IllegalStateException.class);
        }
    }

    private MockEnvironment ambienteSeguro() {
        return new MockEnvironment()
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("spring.sql.init.mode", "never");
    }

    @ParameterizedTest
    @ValueSource(strings = {"WMS_PROD", "WMS_TEST", "wms_dev", "outro"})
    void recusaQualquerBancoDiferenteDoDevAutorizado(String banco) {
        var p =
                new SqlServerProperties(
                        "sql.test.invalid",
                        1433,
                        banco,
                        "usuario-ficticio",
                        "senha-ficticia",
                        "sql.test.invalid:1433/" + banco,
                        "SQL-FICTICIO");
        assertThatThrownBy(p::validarAlvo).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void hikariAplicaGuardaEmDuasConexoesFisicas() throws Exception {
        var aplicadas = new CopyOnWriteArrayList<String>();
        var config = new SqlServerConfig().configuracao(propriedades(), ambienteSeguro());
        DataSource fonte = mock(DataSource.class);
        when(fonte.getConnection(anyString(), anyString()))
                .thenAnswer(
                        invocacao -> {
                            Connection conexao = mock(Connection.class);
                            Statement statement = mock(Statement.class);
                            when(conexao.getAutoCommit()).thenReturn(true);
                            when(conexao.isValid(org.mockito.ArgumentMatchers.anyInt()))
                                    .thenReturn(true);
                            when(conexao.createStatement()).thenReturn(statement);
                            when(statement.execute(anyString()))
                                    .thenAnswer(
                                            sql -> {
                                                aplicadas.add(sql.getArgument(0));
                                                return false;
                                            });
                            return conexao;
                        });
        config.setDataSource(fonte);
        config.setMinimumIdle(0);
        config.setMaximumPoolSize(2);
        try (var pool = new HikariDataSource(config);
                var primeira = pool.getConnection();
                var segunda = pool.getConnection()) {
            assertThat(primeira).isNotSameAs(segunda);
            assertThat(aplicadas)
                    .hasSizeGreaterThanOrEqualTo(2)
                    .allMatch(config.getConnectionInitSql()::equals);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {50001, 50002})
    void poolNaoEExpostoQuandoGuardaDeIdentidadeOuPrivilegioFalha(int codigo) throws Exception {
        DataSource fonte = mock(DataSource.class);
        Connection conexao = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(fonte.getConnection(anyString(), anyString())).thenReturn(conexao);
        when(conexao.getAutoCommit()).thenReturn(true);
        when(conexao.createStatement()).thenReturn(statement);
        when(statement.execute(anyString()))
                .thenThrow(new SQLException("Guarda fictícia recusada.", "S0001", codigo));
        var config = new SqlServerConfig().configuracao(propriedades(), ambienteSeguro());
        config.setDataSource(fonte);
        config.setInitializationFailTimeout(1);
        assertThatThrownBy(() -> new HikariDataSource(config))
                .hasRootCauseInstanceOf(SQLException.class)
                .hasRootCauseMessage("Guarda fictícia recusada.");
    }

    private SqlServerProperties propriedades() {
        return new SqlServerProperties(
                "sql.test.invalid",
                1433,
                "WMS_DEV",
                "usuario-ficticio",
                "senha-ficticia",
                "sql.test.invalid:1433/WMS_DEV",
                "SQL-FICTICIO");
    }
}
