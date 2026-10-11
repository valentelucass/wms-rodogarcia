package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import tools.jackson.databind.json.JsonMapper;

/** Driver/pool/JDBC sao mocks: nao abre pool, TCP, H2 ou SQL Server. */
class Sql01JdbcObservationTest {
    @TempDir Path output;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void observaProviderEIdentidadePelaConexaoDoSutSemDadosOuSegredos() throws Exception {
        var pool = pool("WMS_DEV");
        new Sql01JdbcObservation(output, mapper, environment()).registrar(pool);
        var json = mapper.readTree(Files.readString(output.resolve("jdbc-provider.json")));
        assertThat(json.get("approved").asBoolean()).isTrue();
        assertThat(json.get("provider").asString()).isEqualTo("SQLServer");
        assertThat(json.get("database").asString()).isEqualTo("WMS_DEV");
        assertThat(json.get("login").asString()).isEqualTo("WMSDEV");
        assertThat(json.get("user").asString()).isEqualTo("WMSDEV");
        assertThat(json.get("metadataStatementsExecuted").asInt()).isEqualTo(1);
        assertThat(json.has("rows")).isFalse();
        assertThat(json.has("password")).isFalse();
        org.mockito.Mockito.verify(pool.getConnection()).close();
        org.mockito.Mockito.verify(pool.getConnection().createStatement())
                .executeQuery("SELECT DB_NAME(), ORIGINAL_LOGIN(), USER_NAME(), @@SPID");
    }

    @Test
    void recusaOutroBancoESalvaNegativoSemAceiteSql() throws Exception {
        var pool = pool("WMS_PROD");
        assertThatThrownBy(
                        () ->
                                new Sql01JdbcObservation(output, mapper, environment())
                                        .registrar(pool))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SQL01_JDBC_PROVIDER_IDENTITY_REFUSED");
        var json = mapper.readTree(Files.readString(output.resolve("jdbc-provider.json")));
        assertThat(json.get("approved").asBoolean()).isFalse();
        assertThat(json.has("provider")).isFalse();
        assertThat(json.get("database").asString()).isEqualTo("WMS_PROD");
    }

    @Test
    void erroJdbcNaoVazaMensagemENaoRetenta() throws Exception {
        var pool = pool("WMS_DEV");
        when(pool.getConnection()).thenThrow(new SQLException("SEGREDO_PASSWORD_TOKEN"));
        assertThatThrownBy(
                        () ->
                                new Sql01JdbcObservation(output, mapper, environment())
                                        .registrar(pool))
                .hasMessage("SQL01_JDBC_PROVIDER_IDENTITY_REFUSED");
        String json = Files.readString(output.resolve("jdbc-provider.json"));
        assertThat(json).doesNotContain("SEGREDO_PASSWORD_TOKEN");
        assertThat(mapper.readTree(json).get("metadataStatementsExecuted").asInt()).isZero();
        org.mockito.Mockito.verify(pool).getConnection();
    }

    @Test
    void arquivoAnteriorOuHashInvalidoRecusaAntesConexao() throws Exception {
        var pool = mock(HikariDataSource.class);
        Files.writeString(output.resolve("jdbc-provider.json"), "anterior");
        assertThatThrownBy(
                        () ->
                                new Sql01JdbcObservation(output, mapper, environment())
                                        .registrar(pool))
                .isInstanceOf(java.nio.file.FileAlreadyExistsException.class);
        assertThat(Files.readString(output.resolve("jdbc-provider.json"))).isEqualTo("anterior");
        var env = environment().withProperty("wms.sql01.candidate-jar-sha256", "invalid");
        assertThatThrownBy(() -> new Sql01JdbcObservation(output, mapper, env))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(pool);
    }

    private MockEnvironment environment() {
        return new MockEnvironment()
                .withProperty("wms.sql01.run-id", "6e788f5e-6d00-476d-80fa-5ed10fc0926d")
                .withProperty("wms.sql01.candidate-source-hash", "a".repeat(64))
                .withProperty("wms.sql01.candidate-jar-sha256", "b".repeat(64));
    }

    private HikariDataSource pool(String database) throws SQLException {
        var pool = mock(HikariDataSource.class);
        when(pool.getJdbcUrl())
                .thenReturn(
                        "jdbc:sqlserver://127.0.0.1:1433;databaseName=WMS_DEV;encrypt=true;trustServerCertificate=false");
        when(pool.getUsername()).thenReturn("WMSDEV");
        var properties = new Properties();
        properties.put("trustStore", "privado-ficticio.p12");
        properties.put("hostNameInCertificate", "LOCAL-FICTICIO");
        when(pool.getDataSourceProperties()).thenReturn(properties);
        var connection = mock(Connection.class);
        var metadata = mock(DatabaseMetaData.class);
        var statement = mock(Statement.class);
        var rows = mock(ResultSet.class);
        when(pool.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metadata);
        when(connection.createStatement()).thenReturn(statement);
        when(metadata.getDatabaseProductName()).thenReturn("Microsoft SQL Server");
        when(metadata.getDriverName()).thenReturn("Microsoft JDBC Driver");
        when(metadata.getDriverVersion()).thenReturn("13.2.0");
        when(statement.executeQuery("SELECT DB_NAME(), ORIGINAL_LOGIN(), USER_NAME(), @@SPID"))
                .thenReturn(rows);
        when(rows.next()).thenReturn(true, false);
        when(rows.getString(1)).thenReturn(database);
        when(rows.getString(2)).thenReturn("WMSDEV");
        when(rows.getString(3)).thenReturn("WMSDEV");
        when(rows.getInt(4)).thenReturn(72);
        return pool;
    }
}
