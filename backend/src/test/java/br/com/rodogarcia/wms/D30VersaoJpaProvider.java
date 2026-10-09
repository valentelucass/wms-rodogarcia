package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Dois contexts JPA locais; metadado de versão separado da memória financeira imutável. */
final class D30VersaoJpaProvider {
    private D30VersaoJpaProvider() {}

    static <T> void conferir(
            EntityManagerFactory emf,
            JdbcTemplate jdbc,
            Class<T> tipo,
            String tabela,
            Long id,
            String caso)
            throws Exception {
        assertThat(System.getProperty("wms.test.local.guard")).isEqualTo("D30");
        assertThat(emf.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect())
                .isInstanceOf(H2Dialect.class);
        assertThat(tabela).isIn("AJUSTE_FECHAMENTO", "VERSAO_FECHAMENTO");
        var persister =
                emf.unwrap(SessionFactoryImplementor.class)
                        .getMappingMetamodel()
                        .getEntityDescriptor(tipo);
        var antes = D30FotografiaFisica.capturar(jdbc);
        var original =
                antes.get(tabela).stream()
                        .filter(r -> ((Number) r.get("ID")).longValue() == id)
                        .findFirst()
                        .orElseThrow();
        long versaoAntes = ((Number) original.get("VERSAO")).longValue();
        long esperado = Math.addExact(versaoAntes, 1);
        var primeiro = emf.createEntityManager();
        var obsoleto = emf.createEntityManager();
        Throwable falha;
        try {
            T entidade1 = primeiro.find(tipo, id);
            T entidade2 = obsoleto.find(tipo, id);
            assertThat(persister.getVersion(entidade1)).isEqualTo(versaoAntes);
            assertThat(persister.getVersion(entidade2)).isEqualTo(versaoAntes);
            primeiro.getTransaction().begin();
            primeiro.lock(entidade1, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            primeiro.getTransaction().commit();
            var previsto = new LinkedHashMap<>(antes);
            List<Map<String, Object>> linhas = new ArrayList<>();
            for (var linha : antes.get(tabela)) {
                var copia = new LinkedHashMap<>(linha);
                if (((Number) copia.get("ID")).longValue() == id) copia.put("VERSAO", esperado);
                linhas.add(copia);
            }
            previsto.put(tabela, linhas);
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(previsto);
            obsoleto.getTransaction().begin();
            falha =
                    catchThrowable(
                            () -> {
                                obsoleto.lock(entidade2, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
                                obsoleto.getTransaction().commit();
                            });
            assertThat(falha).isNotNull();
            List<Throwable> causas = new ArrayList<>();
            for (Throwable t = falha; t != null; t = t.getCause()) causas.add(t);
            assertThat(causas)
                    .anyMatch(
                            t ->
                                    t instanceof OptimisticLockException
                                            || t instanceof StaleObjectStateException);
            if (obsoleto.getTransaction().isActive()) obsoleto.getTransaction().rollback();
            assertThat(D30FotografiaFisica.capturar(jdbc)).isEqualTo(previsto);
        } finally {
            if (primeiro.getTransaction().isActive()) primeiro.getTransaction().rollback();
            if (obsoleto.getTransaction().isActive()) obsoleto.getTransaction().rollback();
            primeiro.close();
            obsoleto.close();
        }
        Map<String, Object> prova = new LinkedHashMap<>();
        prova.put("caso", caso);
        prova.put("modelo", tipo.getName());
        prova.put("tabela", tabela);
        prova.put("id", id);
        prova.put("versaoAntes", versaoAntes);
        prova.put("versaoEsperadaDepois", esperado);
        prova.put("incrementoExato", true);
        prova.put("versaoObsoletaRecusada", true);
        prova.put("demaisCampos64Conservados", true);
        prova.put("causa", falha.getClass().getName());
        prova.put("SQLServer", false);
        prova.put(
                "limite",
                "Somente mecanismo @Version Hibernate/H2; número da versão financeira/memória/valores não alterados. Não comprova concorrência SQLServer ou operação de negócio nova.");
        Path destino =
                Path.of(System.getProperty("wms.test.evidencias.dir")).toAbsolutePath().normalize();
        assertThat(destino.toString()).contains("d30-cedro-");
        Files.writeString(
                destino.resolve("d30-cedro-versao-jpa-" + UUID.randomUUID() + ".json"),
                JsonMapper.builder()
                        .build()
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(prova),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW);
    }
}
