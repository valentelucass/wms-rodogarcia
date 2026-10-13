package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Lê o snapshot imutável da chegada, já gravado junto com seus itens na mesma transação. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
@Transactional(propagation = Propagation.MANDATORY)
public class ConferenciaBobinasService {
    private final AuditoriaCadastroRepository auditoria;
    private final JsonMapper mapper;

    public ConferenciaBobinasService(AuditoriaCadastroRepository auditoria, JsonMapper mapper) {
        this.auditoria = auditoria;
        this.mapper = mapper;
    }

    public static void validar(RecebimentoDto.Item item) {
        var b = item.bobinas();
        if (b == null) return;
        if (b.boas() == null || b.avariadas() == null || b.boas() < 0 || b.avariadas() < 0
                || b.boas() + b.avariadas() < 1 || b.boas() + b.avariadas() > 100
                || (b.boas() == 0) != (item.quantidadeBoa().signum() == 0)
                || (b.avariadas() == 0) != (item.quantidadeAvariada().signum() == 0))
            throw CadastroSupport.invalido("Informe de 1 a 100 bobinas por item, separando boas e avariadas conforme as quantidades conferidas.");
    }

    /** O chamador autoriza e consulta o pedido antes desta leitura; uma consulta por pedido. */
    public Map<Long, Map<Integer, RecebimentoDto.Bobinas>> consultar(Long pedidoId) {
        var resultado = new HashMap<Long, Map<Integer, RecebimentoDto.Bobinas>>();
        for (var a : auditoria.findByTipoAndRegistroIdAndAcaoOrderByIdAsc(
                "PEDIDO_ENTRADA", pedidoId, "CHEGADA_REGISTRADA")) {
            var evento = mapper.readTree(a.getDadosDepois()).path("evento");
            var contagens = new HashMap<Integer, RecebimentoDto.Bobinas>();
            int sequencia = 0;
            for (var item : evento.path("dados").path("itens")) {
                sequencia++;
                if (item.hasNonNull("bobinas"))
                    contagens.put(sequencia, mapper.treeToValue(item.get("bobinas"), RecebimentoDto.Bobinas.class));
            }
            if (!contagens.isEmpty()) resultado.put(evento.path("chegadaId").asLong(), contagens);
        }
        return resultado;
    }

    public static RecebimentoDto.Bobinas obter(Map<Long, Map<Integer, RecebimentoDto.Bobinas>> contagens, ItemChegada item) {
        return contagens.getOrDefault(item.getChegada().getId(), Map.of()).get(item.getSequencia());
    }

    public static void conferir(RecebimentoDto.Bobinas contagem, List<UnidadeLogisticaDto.NovaUnidade> unidades) {
        if (contagem == null) return; // histórico sem contagem física não recebe número presumido
        long boas = unidades.stream().filter(u -> u.condicao() == CondicaoMercadoria.BOA).count();
        if (unidades.stream().anyMatch(u -> u.tipo() != TipoUnidadeLogistica.BOBINA)
                || boas != contagem.boas() || unidades.size() - boas != contagem.avariadas())
            throw RegraNegocioException.conflito("CONTAGEM_BOBINAS_DIVERGENTE",
                    "As bobinas distribuídas devem corresponder à contagem física de boas e avariadas registrada na chegada.");
    }
}
