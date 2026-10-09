package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.mapping.Table;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import tools.jackson.databind.json.JsonMapper;

/** F01: metadado real e schema H2 efemero, sem migrations ou SQL Server. */
class D30DocumentoSaidaSchemaLocalTest {
    @Test
    void variasChavesAusentesEChaveInformadaUnicaNoSchemaLocal() throws Exception {
        var registry =
                new StandardServiceRegistryBuilder()
                        .applySettings(
                                Map.of(
                                        "hibernate.dialect",
                                        "org.hibernate.dialect.H2Dialect",
                                        "hibernate.boot.allow_jdbc_metadata_access",
                                        "false",
                                        "hibernate.hbm2ddl.auto",
                                        "none"))
                        .build();
        try {
            var sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var candidate : scanner.findCandidateComponents("br.com.rodogarcia.wms.models")) {
                sources.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
            }
            var metadata = sources.buildMetadata();
            Table documento = null;
            for (var namespace : metadata.getDatabase().getNamespaces()) {
                for (var table : namespace.getTables()) {
                    if (table.getName().equals("documento_saida")) documento = table;
                }
            }
            assertThat(documento).isNotNull();
            var chave =
                    documento.getColumns().stream()
                            .filter(c -> c.getName().equals("chave_acesso"))
                            .findFirst()
                            .orElseThrow();
            assertThat(chave.isNullable()).isTrue();
            var unique = documento.getUniqueKeys().get("uk_documento_saida_chave");
            assertThat(unique).isNotNull();
            assertThat(unique.getColumns())
                    .extracting(c -> c.getName())
                    .containsExactly("chave_acesso");
            var context =
                    SqlStringGenerationContextImpl.fromConfigurationMap(
                            registry.getService(JdbcEnvironment.class),
                            metadata.getDatabase(),
                            Map.of());
            var ddl =
                    metadata.getDatabase()
                            .getDialect()
                            .getTableExporter()
                            .getSqlCreateStrings(documento, metadata, context);
            assertThat(String.join("\n", ddl))
                    .contains("uk_documento_saida_chave")
                    .doesNotContain("WHERE chave_acesso IS NOT NULL");
            String name = "d30-cedro-f01-" + UUID.randomUUID();
            try (var conn =
                    DriverManager.getConnection(
                            "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=0", "sa", "")) {
                assertThat(conn.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:" + name);
                assertThat(conn.getMetaData().getDatabaseProductName()).isEqualTo("H2");
                try (var statement = conn.createStatement()) {
                    statement.execute("CREATE SCHEMA wms");
                    for (String command : ddl) statement.execute(command);
                }
                inserir(conn, "101", null);
                inserir(conn, "102", null);
                String informada = "1".repeat(44);
                inserir(conn, "103", informada);
                var erro = catchThrowable(() -> inserir(conn, "104", informada));
                assertThat(erro).isInstanceOf(SQLException.class);
                assertThat(((SQLException) erro).getSQLState()).isEqualTo("23505");
                try (var statement = conn.createStatement();
                        var result =
                                statement.executeQuery(
                                        "SELECT COUNT(*), COUNT(chave_acesso) FROM wms.documento_saida")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getInt(1)).isEqualTo(3);
                    assertThat(result.getInt(2)).isEqualTo(1);
                }
                var proof =
                        Map.of(
                                "id",
                                "D30-PRUMO-F01",
                                "metadataURL",
                                conn.getMetaData().getURL(),
                                "jpaNullable",
                                true,
                                "jpaUniqueSemFiltro",
                                true,
                                "ddlH2Gerado",
                                Arrays.asList(ddl),
                                "documentosSemChave",
                                2,
                                "chaveInformadaUnica",
                                1,
                                "duplicacaoSQLState",
                                "23505",
                                "SQLServer",
                                false,
                                "limite",
                                "Tabela do metadado real isolada, sem FKs/camadas operacionais. H2 UNIQUE admite variosNULL e recusa repetida; nao equivale descritorSQLServer filtradoV6 nem prova operacaoSQLServer. Garantia local mantida; nenhuma correcao de producao inferida.");
                var folder = Path.of(System.getProperty("wms.test.evidencias.dir", "evidencias"));
                Files.createDirectories(folder);
                Files.writeString(
                        folder.resolve("d30-cedro-f01-" + UUID.randomUUID() + ".json"),
                        JsonMapper.builder()
                                .build()
                                .writerWithDefaultPrettyPrinter()
                                .writeValueAsString(proof),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE);
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    private static void inserir(Connection conn, String numero, String chave) throws SQLException {
        try (var statement =
                conn.prepareStatement(
                        "INSERT INTO wms.documento_saida "
                                + "(pedido_id,origem,natureza,emitente_cnpj,serie,numero,emissao,chave_acesso,protocolo,situacao,registrado_em) VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
            statement.setLong(1, 1L);
            statement.setString(2, "XML");
            statement.setString(3, "RETORNO_MERCADORIA");
            statement.setString(4, "11111111000111");
            statement.setString(5, "1");
            statement.setString(6, numero);
            statement.setObject(7, LocalDate.of(2026, 9, 1));
            statement.setString(8, chave);
            statement.setString(9, "D30-ficticio");
            statement.setString(10, "AUTORIZADO");
            statement.setObject(11, Instant.parse("2026-09-01T00:00:00Z"));
            statement.executeUpdate();
        }
    }
}
