package br.com.rodogarcia.wms.config;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

/** Uma leitura de metadados pelo pool do SUT; nao modifica dados, esquema ou transacoes. */
final class Sql01JdbcObservation {
    private static final String URL =
            "jdbc:sqlserver://127.0.0.1:1433;databaseName=WMS_DEV;encrypt=true;trustServerCertificate=false";
    private final Path output;
    private final JsonMapper mapper;
    private final String run;
    private final String sourceHash;
    private final String jarHash;

    Sql01JdbcObservation(Path output, JsonMapper mapper, Environment environment) {
        this.output = output;
        this.mapper = mapper;
        run = UUID.fromString(environment.getRequiredProperty("wms.sql01.run-id")).toString();
        sourceHash = hash(environment, "wms.sql01.candidate-source-hash");
        jarHash = hash(environment, "wms.sql01.candidate-jar-sha256");
    }

    private static String hash(Environment environment, String property) {
        String value = environment.getRequiredProperty(property);
        if (!value.matches("[a-fA-F0-9]{64}"))
            throw new IllegalStateException("SQL01_HASH_INVALIDO");
        return value.toLowerCase(java.util.Locale.ROOT);
    }

    void registrar(DataSource dataSource) throws IOException {
        if (Files.exists(output.resolve("jdbc-provider.json"))) {
            throw new java.nio.file.FileAlreadyExistsException("SQL01_JDBC_EVIDENCE_EXISTS");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", "QUAL-CONF01-SQL01");
        result.put("runId", run);
        result.put("pid", ProcessHandle.current().pid());
        result.put("startedUtc", Instant.now().toString());
        result.put("declaredSourceHash", sourceHash);
        result.put("declaredJarSha256", jarHash);
        result.put("scope", "SUT_CONNECTION_METADATA_ONLY_NOT_FUNCTIONAL_TEST");
        result.put("metadataStatementsExecuted", 0);
        boolean approved = false;
        try {
            if (!(dataSource instanceof HikariDataSource pool)
                    || !URL.equals(pool.getJdbcUrl())
                    || !"WMSDEV".equals(pool.getUsername())
                    || pool.getDataSourceProperties().containsKey("encrypt")
                    || pool.getDataSourceProperties().containsKey("trustServerCertificate")
                    || !(pool.getDataSourceProperties().get("trustStore")
                            instanceof String trustStore)
                    || trustStore.isBlank()
                    || !(pool.getDataSourceProperties().get("hostNameInCertificate")
                            instanceof String host)
                    || host.isBlank()) {
                throw new SQLException("SQL01_POOL_RESTRITO_INVALIDO");
            }
            try (var connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
                var metadata = connection.getMetaData();
                String product = metadata.getDatabaseProductName();
                result.put("databaseProductName", seguro(product));
                result.put("driverName", seguro(metadata.getDriverName()));
                result.put("driverVersion", seguro(metadata.getDriverVersion()));
                statement.setQueryTimeout(10);
                try (var rows =
                        statement.executeQuery(
                                "SELECT DB_NAME(), ORIGINAL_LOGIN(), USER_NAME(), @@SPID")) {
                    result.put("metadataStatementsExecuted", 1);
                    if (!rows.next()) throw new SQLException("SQL01_METADATA_AUSENTE");
                    String database = rows.getString(1);
                    String login = rows.getString(2);
                    String user = rows.getString(3);
                    result.put("database", seguro(database));
                    result.put("login", seguro(login));
                    result.put("user", seguro(user));
                    result.put("sessionId", rows.getInt(4));
                    approved =
                            "Microsoft SQL Server".equals(product)
                                    && "WMS_DEV".equals(database)
                                    && "WMSDEV".equals(login)
                                    && "WMSDEV".equals(user)
                                    && !rows.next();
                }
                result.put("connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse", true);
                result.put("privateTrustStoreAndCertificateHostConfigured", true);
            }
        } catch (SQLException | RuntimeException failure) {
            approved = false;
            result.put("failureType", failure.getClass().getSimpleName());
        }
        result.put("approved", approved);
        if (approved) result.put("provider", "SQLServer");
        result.put("finishedUtc", Instant.now().toString());
        result.put(
                "limit",
                "TLS binding from guarded pool configuration and successful driver connection; no DMV, SQLCPU, user rows, URL, credentials or binds recorded. Source/artifact declarations require independent runtime hash comparison.");
        Files.writeString(
                output.resolve("jdbc-provider.json"),
                mapper.writeValueAsString(result),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
        if (!approved) throw new IllegalStateException("SQL01_JDBC_PROVIDER_IDENTITY_REFUSED");
    }

    private static String seguro(String value) {
        return value != null && value.length() <= 200 && value.matches("[A-Za-z0-9 ._()/-]+")
                ? value
                : "<unavailable>";
    }
}
