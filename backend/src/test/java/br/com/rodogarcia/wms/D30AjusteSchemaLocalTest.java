package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import br.com.rodogarcia.wms.models.AjusteFechamento;
import jakarta.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

/** A10: somente metadado real e tabela H2 propria; nao executa migrations ou SQL Server. */
class D30AjusteSchemaLocalTest {
    @Test
    void ajustesComunsSemTratativaERegularizacaoUnicaConservamCheckLocal() throws Exception {
        var declared = AjusteFechamento.class.getAnnotation(jakarta.persistence.Table.class);
        assertThat(declared.indexes()).hasSize(1);
        assertThat(declared.indexes()[0].name()).isEqualTo("uk_ajuste_tratativa_origem");
        assertThat(declared.indexes()[0].columnList()).isEqualTo("tratativa_origem_id");
        assertThat(declared.indexes()[0].unique()).isTrue();
        var registry =
                new StandardServiceRegistryBuilder()
                        .applySettings(
                                Map.of(
                                        "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
                                        "hibernate.boot.allow_jdbc_metadata_access", "false",
                                        "hibernate.hbm2ddl.auto", "none"))
                        .build();
        try {
            var sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var candidate : scanner.findCandidateComponents("br.com.rodogarcia.wms.models")) {
                sources.addAnnotatedClass(Class.forName(candidate.getBeanClassName()));
            }
            var metadata = sources.buildMetadata();
            Table ajuste = null;
            for (var namespace : metadata.getDatabase().getNamespaces()) {
                for (var table : namespace.getTables()) {
                    if (table.getName().equals("ajuste_fechamento")) ajuste = table;
                }
            }
            assertThat(ajuste).isNotNull();
            var tratativa =
                    ajuste.getColumns().stream()
                            .filter(c -> c.getName().equals("tratativa_origem_id"))
                            .findFirst()
                            .orElseThrow();
            assertThat(tratativa.isNullable()).isTrue();
            var context =
                    SqlStringGenerationContextImpl.fromConfigurationMap(
                            registry.getService(JdbcEnvironment.class),
                            metadata.getDatabase(),
                            Map.of());
            var dialect = metadata.getDatabase().getDialect();
            var ddl =
                    new ArrayList<String>(
                            List.of(
                                    dialect.getTableExporter()
                                            .getSqlCreateStrings(ajuste, metadata, context)));
            for (var index : ajuste.getIndexes().values()) {
                ddl.addAll(
                        List.of(
                                dialect.getIndexExporter()
                                        .getSqlCreateStrings(index, metadata, context)));
            }
            assertThat(String.join("\n", ddl))
                    .contains("uk_ajuste_tratativa_origem")
                    .contains("CORRECAO_CALCULO", "REGULARIZACAO_ORIGEM")
                    .doesNotContain("WHERE tratativa_origem_id IS NOT NULL");
            String name = "d30-cedro-a10-" + UUID.randomUUID();
            try (var conn =
                    DriverManager.getConnection(
                            "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=0", "sa", "")) {
                assertThat(conn.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:" + name);
                assertThat(conn.getMetaData().getDatabaseProductName()).isEqualTo("H2");
                try (var statement = conn.createStatement()) {
                    statement.execute("CREATE SCHEMA wms");
                    for (String command : ddl) statement.execute(command);
                }
                inserir(conn, "CORRECAO_CALCULO", null, "1");
                inserir(conn, "CORRECAO_CALCULO", null, "2");
                inserir(conn, "REGULARIZACAO_ORIGEM", 77L, "3");
                exigirEstado(() -> inserir(conn, "REGULARIZACAO_ORIGEM", 77L, "4"), "23505");
                exigirEstado(() -> inserir(conn, "CORRECAO_CALCULO", 88L, "5"), "23513");
                exigirEstado(() -> inserir(conn, "REGULARIZACAO_ORIGEM", null, "6"), "23513");
                try (var statement = conn.createStatement();
                        var rs =
                                statement.executeQuery(
                                        "SELECT COUNT(*),COUNT(tratativa_origem_id) FROM wms.ajuste_fechamento")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getInt(1)).isEqualTo(3);
                    assertThat(rs.getInt(2)).isEqualTo(1);
                }
                try (var statement = conn.createStatement();
                        var rs =
                                statement.executeQuery(
                                        "SELECT tratativa_origem_id FROM wms.ajuste_fechamento WHERE tipo='REGULARIZACAO_ORIGEM'")) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getLong(1)).isEqualTo(77L);
                    assertThat(rs.next()).isFalse();
                }
                var proof =
                        Map.of(
                                "id",
                                "D30-C-CASO-AJUSTE-SCHEMA-001",
                                "URL",
                                conn.getMetaData().getURL(),
                                "ddlH2DoMetadadoReal",
                                ddl,
                                "comunsComTratativaNull",
                                2,
                                "regularizacoesComTratativa77",
                                1,
                                "duplicacaoSQLState",
                                "23505",
                                "checkSQLState",
                                "23513",
                                "SQLServer",
                                false,
                                "limite",
                                "Uma tabela H2 do metadado real, sem FKs/camadas operacionais. UNIQUE nao filtrado admite NULL multiplos e recusa tratativa repetida; CHECK conserva tipo/null. Nao comprova filtro V8, driver SQL Server ou identificarRegularizacao; garantias locais preservadas.");
                var dir = Path.of(System.getProperty("wms.test.evidencias.dir", "evidencias"));
                Files.createDirectories(dir);
                Files.writeString(
                        dir.resolve("d30-cedro-a10-" + UUID.randomUUID() + ".json"),
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

    @FunctionalInterface
    private interface Insercao {
        void executar() throws SQLException;
    }

    private static void exigirEstado(Insercao acao, String estado) {
        var erro = catchThrowable(acao::executar);
        assertThat(erro).isInstanceOf(SQLException.class);
        assertThat(((SQLException) erro).getSQLState()).isEqualTo(estado);
    }

    private static void inserir(Connection conn, String tipo, Long tratativa, String hash)
            throws SQLException {
        try (var s =
                conn.prepareStatement(
                        "INSERT INTO wms.ajuste_fechamento (tipo,tratativa_origem_id,versao,origem_versao_id,destino_fechamento_id,calculo_base_id,calculo_corrigido_id,hash_correcao,valor_base,valor_corrigido,diferenca,situacao,motivo,evidencia,usuario,registrado_em) VALUES (?,?,0,1,1,1,2,?,100,80,-20,'VALIDADO','Motivo ficticio D30','Evidencia ficticia D30','D30-ficticio',?)")) {
            s.setString(1, tipo);
            s.setObject(2, tratativa);
            s.setString(3, hash.repeat(64));
            s.setObject(4, Instant.parse("2026-09-01T12:00:00.123456Z"));
            assertThat(s.executeUpdate()).isEqualTo(1);
        }
    }
}
