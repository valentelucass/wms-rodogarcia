package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.Entity;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.PreparableMutationOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import tools.jackson.databind.json.JsonMapper;

/** SQL preparado pelo dialeto real, sem JDBC, DDL, H2 ou banco externo. */
class SqlServerUpdateMappingTest {
    @Test
    void updatesEstaticosRespeitamColunasMutaveisESnapshots() throws Exception {
        var registry =
                new StandardServiceRegistryBuilder()
                        .applySettings(
                                Map.of(
                                        "hibernate.dialect",
                                                "org.hibernate.dialect.SQLServerDialect",
                                        "hibernate.boot.allow_jdbc_metadata_access", "false",
                                        "hibernate.hbm2ddl.auto", "none"))
                        .build();
        try {
            var sources = new MetadataSources(registry);
            var scanner = new ClassPathScanningCandidateComponentProvider(false);
            scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
            for (var candidato : scanner.findCandidateComponents("br.com.rodogarcia.wms.models")) {
                sources.addAnnotatedClass(Class.forName(candidato.getBeanClassName()));
            }
            try (var factory =
                    (SessionFactoryImplementor) sources.buildMetadata().buildSessionFactory()) {
                Map<String, String> sql = new TreeMap<>();
                factory.getMappingMetamodel()
                        .forEachEntityDescriptor(
                                persister -> {
                                    var grupo =
                                            persister
                                                    .getUpdateCoordinator()
                                                    .getStaticMutationOperationGroup();
                                    if (grupo != null) {
                                        for (int indice = 0;
                                                indice < grupo.getNumberOfOperations();
                                                indice++) {
                                            var operacao = grupo.getOperation(indice);
                                            if (operacao
                                                    instanceof
                                                    PreparableMutationOperation preparada) {
                                                sql.put(
                                                        persister.getMappedClass().getSimpleName(),
                                                        preparada.getSqlString());
                                            }
                                        }
                                    }
                                });
                salvarEvidencia(
                        sql,
                        Path.of(
                                System.getProperty("wms.test.evidencias.dir", "evidencias"),
                                "d30-cedro-hibernate-update-sql-" + UUID.randomUUID() + ".json"));
                assertThat(sql.get("PedidoSaida"))
                        .contains("alterado_em=?", "situacao=?", "versao=?")
                        .doesNotContain(
                                "cliente_id=?", "armazem_id=?", "referencia=?", "criado_em=?");
                assertThat(sql.get("EntradaConferida"))
                        .contains("unitizada_em=?")
                        .doesNotContain("quantidade=?", "fifo_em=?", "nota_id=?");
                assertThat(sql.get("ReservaSaida"))
                        .contains("situacao=?", "encerrada_em=?")
                        .doesNotContain("quantidade=?", "unidade_id=?", "item_id=?");
                assertThat(sql.get("VersaoFechamento"))
                        .doesNotContain(
                                "memoria_json=?", "conteudo_hash=?", "saldo=?", "calculo_id=?");
                assertThat(sql.get("ChegadaRecebimento"))
                        .isEqualTo(
                                "update wms.chegada_recebimento set estornada_em=?,estornada_por=?,motivo_estorno=? where id=?");
                assertThat(sql.get("ItemNotaEntrada"))
                        .isEqualTo(
                                "update wms.item_nota_entrada set valor_mercadoria=? where id=?");
                assertThat(sql.get("NotaEntrada"))
                        .isEqualTo(
                                "update wms.nota_entrada set chave_acesso=?,xml_hash=?,xml_original=? where id=?");
                assertThat(sql.get("PedidoEntrada")).doesNotContain("referencia=?", "criado_em=?");
                assertThat(sql)
                        .doesNotContainKeys(
                                "AuditoriaCadastro",
                                "ItemChegada",
                                "ItemPedidoSaida",
                                "OperacaoSaida",
                                "OperacaoUnidade",
                                "MovimentoEstoque",
                                "OperacaoAdministrativa",
                                "CalculoCobranca",
                                "MemoriaDiaria",
                                "MemoriaServico",
                                "RevisaoCargaInicial",
                                "ResolucaoRemanescente",
                                "ReferenciaNfse",
                                "TratativaExternaFechamento");
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void preservaBytesDaEvidenciaPreexistente(@TempDir Path isolado) throws Exception {
        var destino = isolado.resolve("evidencia-anterior.json");
        var bytesAnteriores = "{\"historico\":\"imutavel\"}\n".getBytes(StandardCharsets.UTF_8);
        Files.write(destino, bytesAnteriores, StandardOpenOption.CREATE_NEW);
        assertThatThrownBy(() -> salvarEvidencia(Map.of("Novo", "sql-estatico"), destino))
                .isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readAllBytes(destino)).isEqualTo(bytesAnteriores);
    }

    private static void salvarEvidencia(Map<String, String> sql, Path destino) throws Exception {
        Files.createDirectories(destino.getParent());
        Files.writeString(
                destino,
                JsonMapper.builder()
                        .build()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(sql),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }
}
