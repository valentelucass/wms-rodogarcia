package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class HistoricoOperacaoTemporalService {
    private final MovimentoEstoqueRepository movimentos;
    private final OperacaoUnidadeRepository transformacoes;
    private final FatoPermanenciaRepository fatos;
    private final Clock clock;

    public HistoricoOperacaoTemporalService(
            MovimentoEstoqueRepository movimentos,
            OperacaoUnidadeRepository transformacoes,
            FatoPermanenciaRepository fatos,
            Clock clock) {
        this.movimentos = movimentos;
        this.transformacoes = transformacoes;
        this.fatos = fatos;
        this.clock = clock;
    }

    public void conferir(UnidadeLogistica u, Instant ocorrida) {
        if (ocorrida == null
                || ocorrida.isAfter(Instant.now(clock))
                || ocorrida.isBefore(u.getCriadaEm())
                || ocorrida.isBefore(u.getChegadaReal())) throw insuficiente();
        if (movimentos.findByUnidadeIdOrderByInstanteAscIdAsc(u.getId()).stream()
                        .anyMatch(m -> m.getInstante().isAfter(ocorrida))
                || fatos.findByUnidadeIdOrderByOcorridaEmAscIdAsc(u.getId()).stream()
                        .anyMatch(f -> f.getOcorridaEm().isAfter(ocorrida))
                || transformacoes
                        .findByPedidoIdAndTipoInOrderByRegistradaEmAscIdAsc(
                                u.getPedido().getId(),
                                List.of("UNIDADE_DIVIDIDA", "UNIDADES_REAGRUPADAS"))
                        .stream()
                        .anyMatch(t -> t.getRegistradaEm().isAfter(ocorrida))) throw insuficiente();
    }

    private static RegraNegocioException insuficiente() {
        return RegraNegocioException.conflito(
                "HISTORICO_CONTINGENCIA_INSUFICIENTE",
                "Origem ou movimento posterior não comprova o estado físico no instante informado; regularize a linha temporal.");
    }
}
