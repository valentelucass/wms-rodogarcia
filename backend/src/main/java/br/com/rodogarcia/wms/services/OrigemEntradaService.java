package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.repositories.OperacaoAdministrativaRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** A origem provém do recibo de criação, nunca da vinculação posterior de um XML. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class OrigemEntradaService {
    public static final String RECURSO = "PEDIDO_ENTRADA_XML";
    private final OperacaoAdministrativaRepository operacoes;
    private final JsonMapper mapper;

    public OrigemEntradaService(OperacaoAdministrativaRepository operacoes, JsonMapper mapper) {
        this.operacoes = operacoes;
        this.mapper = mapper;
    }

    public Map<Long, String> consultar(Collection<PedidoEntrada> pedidos) {
        var resultado = new HashMap<Long, String>();
        var porId = new HashMap<Long, PedidoEntrada>();
        pedidos.forEach(p -> porId.put(p.getId(), p));
        if (porId.isEmpty()) return resultado;
        for (var op : operacoes.findByTipoAndRecursoIdIn("CRIACAO", porId.keySet())) {
            var json = mapper.readTree(op.getResultado());
            if (!RECURSO.equals(json.path("tipoRecurso").asString())) continue;
            var p = porId.get(op.getRecursoId());
            if (op.getCliente() != null
                    && op.getArmazem() != null
                    && p.getCliente().getId().equals(op.getCliente().getId())
                    && p.getArmazem().getId().equals(op.getArmazem().getId())
                    && json.path("pedido").path("id").asLong() == p.getId()) {
                resultado.put(p.getId(), "XML");
            }
        }
        return resultado;
    }
}
