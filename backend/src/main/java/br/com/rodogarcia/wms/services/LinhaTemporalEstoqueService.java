package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reconstrói quantidade/equivalência por fatos físicos; não usa saldo de hoje para todo passado.
 */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class LinhaTemporalEstoqueService {
    private final FatoPermanenciaRepository fatos;
    private final OperacaoUnidadeRepository transformacoes;
    private final JsonMapper mapper;

    public LinhaTemporalEstoqueService(
            FatoPermanenciaRepository fatos,
            OperacaoUnidadeRepository transformacoes,
            JsonMapper mapper) {
        this.fatos = fatos;
        this.transformacoes = transformacoes;
        this.mapper = mapper;
    }

    public record Base(BigDecimal quantidade, BigDecimal equivalencia) {}

    private record Transformacao(Instant registradaEm, BigDecimal quantidade) {}

    @Transactional(
            propagation = Propagation.MANDATORY,
            readOnly = true,
            noRollbackFor = RegraNegocioException.class)
    public Base noInstante(UnidadeLogistica u, Instant instante) {
        return reconstruir(u, instante, false);
    }

    @Transactional(
            propagation = Propagation.MANDATORY,
            readOnly = true,
            noRollbackFor = RegraNegocioException.class)
    public Base fisicoNoInstante(UnidadeLogistica u, Instant instante) {
        return reconstruir(u, instante, true);
    }

    private Base reconstruir(UnidadeLogistica u, Instant instante, boolean aceitarZero) {
        return reconstruir(
                u,
                instante,
                aceitarZero,
                fatos.findByUnidadeIdOrderByOcorridaEmAscIdAsc(u.getId()),
                transformacoes.findByPedidoIdAndTipoInOrderByRegistradaEmAscIdAsc(
                        u.getPedido().getId(),
                        List.of("UNIDADE_DIVIDIDA", "UNIDADES_REAGRUPADAS")));
    }

    @Transactional(
            propagation = Propagation.MANDATORY,
            readOnly = true,
            noRollbackFor = RegraNegocioException.class)
    public Base fisicoNoInstante(
            UnidadeLogistica u,
            Instant instante,
            List<br.com.rodogarcia.wms.models.FatoPermanencia> fatosCarregados,
            List<br.com.rodogarcia.wms.models.OperacaoUnidade> transformacoesCarregadas) {
        return reconstruir(u, instante, true, fatosCarregados, transformacoesCarregadas);
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public java.util.Map<Long, List<br.com.rodogarcia.wms.models.OperacaoUnidade>>
            transformacoesParaIndicador(List<Long> pedidos) {
        if (pedidos.isEmpty()) return java.util.Map.of();
        var resultado =
                new java.util.LinkedHashMap<
                        Long, List<br.com.rodogarcia.wms.models.OperacaoUnidade>>();
        for (var t : transformacoes.buscarParaIndicador(pedidos))
            resultado
                    .computeIfAbsent(t.getPedidoId(), k -> new java.util.ArrayList<>())
                    .add(t.getOperacao());
        return resultado;
    }

    private Base reconstruir(
            UnidadeLogistica u,
            Instant instante,
            boolean aceitarZero,
            List<br.com.rodogarcia.wms.models.FatoPermanencia> fatosCarregados,
            List<br.com.rodogarcia.wms.models.OperacaoUnidade> transformacoesCarregadas) {
        if (instante.isBefore(u.getCriadaEm())) throw pendente();
        var transformacao = ultimaTransformacao(u, transformacoesCarregadas);
        // O resultado BE07 comprova o conteúdo após a confirmação, sem datar o estado anterior.
        // Não projetar essa quantidade para antes da última transformação da identidade.
        if (transformacao != null && instante.isBefore(transformacao.registradaEm()))
            throw pendente();
        var mudancas =
                fatosCarregados.stream()
                        .filter(f -> List.of("RETIRADA", "AJUSTE_ESTOQUE").contains(f.getTipo()))
                        .toList();
        if (transformacao != null
                && mudancas.stream()
                        .anyMatch(f -> f.getOcorridaEm().isBefore(transformacao.registradaEm())))
            throw pendente();
        BigDecimal corrente =
                transformacao != null
                        ? transformacao.quantidade()
                        : mudancas.isEmpty()
                                ? u.getQuantidade()
                                : mudancas.getFirst().getQuantidadeAntes();
        BigDecimal noInstante = corrente;
        BigDecimal equivalencia =
                u.getInicioArmazenagemEm() == null || instante.isBefore(u.getInicioArmazenagemEm())
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(u.getPosicoesEquivalentes());
        for (var f : mudancas) {
            if (corrente.compareTo(f.getQuantidadeAntes()) != 0) throw pendente();
            corrente = f.getQuantidadeDepois();
            if (!f.getOcorridaEm().isAfter(instante)) {
                noInstante = f.getQuantidadeDepois();
                equivalencia = f.getEquivalenciaDepois();
            } else if (noInstante.signum() > 0
                    && u.getInicioArmazenagemEm() != null
                    && !instante.isBefore(u.getInicioArmazenagemEm())) {
                equivalencia = f.getEquivalenciaAntes();
                break;
            }
        }
        // Conferir cadeia inteira mesmo quando o instante precede a primeira alteração.
        BigDecimal finalConhecido =
                mudancas.isEmpty() ? corrente : mudancas.getLast().getQuantidadeDepois();
        for (int i = 1; i < mudancas.size(); i++)
            if (mudancas.get(i - 1)
                            .getQuantidadeDepois()
                            .compareTo(mudancas.get(i).getQuantidadeAntes())
                    != 0) throw pendente();
        if (finalConhecido.compareTo(u.getQuantidade()) != 0
                || noInstante.signum() < 0
                || !aceitarZero && noInstante.signum() == 0) throw pendente();
        return new Base(noInstante, noInstante.signum() == 0 ? BigDecimal.ZERO : equivalencia);
    }

    private Transformacao ultimaTransformacao(
            UnidadeLogistica u, List<br.com.rodogarcia.wms.models.OperacaoUnidade> operacoes) {
        Transformacao ultima = null;
        for (var operacao : operacoes) {
            UnidadeLogisticaDto.Resultado resultado;
            try {
                resultado =
                        mapper.readValue(
                                operacao.getResultado(), UnidadeLogisticaDto.Resultado.class);
            } catch (RuntimeException e) {
                throw pendente();
            }
            if (resultado == null || resultado.unidades() == null) throw pendente();
            for (var detalhe : resultado.unidades()) {
                if (detalhe == null || detalhe.unidade() == null || detalhe.unidade().id() == null)
                    throw pendente();
                if (detalhe.unidade().id().equals(u.getId())) {
                    var quantidade = detalhe.unidade().quantidade();
                    if (quantidade == null || quantidade.signum() < 0) throw pendente();
                    ultima = new Transformacao(operacao.getRegistradaEm(), quantidade);
                }
            }
        }
        return ultima;
    }

    private static RegraNegocioException pendente() {
        return RegraNegocioException.conflito(
                "HISTORICO_AVARIA_INSUFICIENTE",
                "Base temporal não comprovada para esta identidade física; resolva o histórico antes do reconhecimento financeiro.");
    }
}
