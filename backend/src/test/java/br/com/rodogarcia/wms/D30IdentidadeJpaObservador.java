package br.com.rodogarcia.wms;

import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PreInsertEventListener;
import tools.jackson.databind.json.JsonMapper;

/** Captura a atribuição de identidade no mesmo objeto, somente nos casos H2 da D30. */
public final class D30IdentidadeJpaObservador {
    private static final Set<String> MODELOS =
            Set.of(
                    "AjusteVersaoFechamento",
                    "CoberturaDocumentoSaida",
                    "ConteudoUnidade",
                    "DependenciaContingencia",
                    "DiaFechamento",
                    "FatoFechamento",
                    "MemoriaDiaria",
                    "MemoriaServico",
                    "OcupacaoEndereco",
                    "OperacaoAdministrativa",
                    "OperacaoSaida",
                    "OperacaoUnidade",
                    "RateioFatoServico",
                    "ResolucaoRemanescente",
                    "RevisaoCargaInicial",
                    "ServicoMinimoContrato");

    private D30IdentidadeJpaObservador() {}

    public static void instalar(EntityManagerFactory emf, String contexto) {
        if (!"D30_FINAL".equals(System.getProperty("wms.test.leituras.sem-efeito"))) return;
        if (!"D30".equals(System.getProperty("wms.test.local.guard")))
            throw new IllegalStateException("Observador JPA exige guarda local D30.");
        var factory = emf.unwrap(SessionFactoryImplementor.class);
        if (!(factory.getJdbcServices().getDialect() instanceof H2Dialect))
            throw new IllegalStateException("Observador JPA recusa provider fora de H2.");
        var registry = factory.getServiceRegistry().getService(EventListenerRegistry.class);
        if (registry == null) throw new IllegalStateException("Registry JPA local ausente.");
        Map<Object, Antes> anteriores = Collections.synchronizedMap(new IdentityHashMap<>());
        Set<String> capturados = ConcurrentHashMap.newKeySet();
        registry.appendListeners(
                EventType.PRE_INSERT,
                (PreInsertEventListener)
                        event -> {
                            String modelo = event.getEntity().getClass().getSimpleName();
                            if (!MODELOS.contains(modelo)) return false;
                            String caso = D30CasoLocalListener.caso(contexto);
                            if (capturados.contains(caso + "#" + modelo)) return false;
                            anteriores.put(
                                    event.getEntity(),
                                    new Antes(
                                            modelo,
                                            caso,
                                            event.getId(),
                                            event.getPersister().getIdentifier(event.getEntity())));
                            return false;
                        });
        registry.appendListeners(
                EventType.POST_INSERT,
                (PostInsertEventListener)
                        event -> {
                            Antes antes = anteriores.remove(event.getEntity());
                            if (antes == null) return;
                            if (!capturados.add(antes.caso() + "#" + antes.modelo())) return;
                            Object idObjeto = event.getPersister().getIdentifier(event.getEntity());
                            boolean gerado =
                                    antes.idEvento() == null
                                            && antes.idObjeto() == null
                                            && event.getId() instanceof Long id
                                            && id > 0
                                            && event.getId().equals(idObjeto);
                            Map<String, Object> proof = new LinkedHashMap<>();
                            proof.put("contexto", contexto);
                            proof.put("caso", antes.caso());
                            proof.put("modelo", antes.modelo());
                            proof.put("tipoJava", event.getEntity().getClass().getName());
                            proof.put("idEventoAntes", antes.idEvento());
                            proof.put("idObjetoAntes", antes.idObjeto());
                            proof.put("idEventoDepois", event.getId());
                            proof.put("idObjetoDepois", idObjeto);
                            proof.put("mesmaInstanciaPrePost", true);
                            proof.put("geracaoComprovada", gerado);
                            proof.put(
                                    "oraculo",
                                    "NULL antes; Long positivo atribuído no insert; evento e objeto com mesmo ID.");
                            proof.put("provider", "Hibernate/H2 isolado");
                            proof.put("SQLServer", false);
                            proof.put(
                                    "limite",
                                    "Insert observado; commit/rollback e demais invariantes exigem testemunhas próprias. PK fotografada não substitui esta captura.");
                            salvar(proof);
                            if (!gerado)
                                throw new IllegalStateException(
                                        "Identidade JPA fora do oráculo independente: "
                                                + antes.modelo());
                        });
    }

    private static void salvar(Map<String, Object> proof) {
        String dir = System.getProperty("wms.test.evidencias.dir");
        if (dir == null) throw new IllegalStateException("Destino próprio D30 ausente.");
        Path destino = Path.of(dir).toAbsolutePath().normalize();
        if (!destino.toString().contains("d30-cedro-"))
            throw new IllegalStateException("Destino do observador fora da D30.");
        try {
            Files.createDirectories(destino);
            Files.writeString(
                    destino.resolve("d30-cedro-identidade-jpa-" + UUID.randomUUID() + ".json"),
                    JsonMapper.builder()
                            .build()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(proof),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(
                    "Captura de identidade JPA não foi preservada.", exception);
        }
    }

    private record Antes(String modelo, String caso, Object idEvento, Object idObjeto) {}
}
