package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.MarcoFinanceiroAvaria;
import br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.MarcoFinanceiroAvariaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class MarcoFinanceiroAvariaService {
    private final MarcoFinanceiroAvariaRepository marcos;
    private final AvariaEstoqueRepository avarias;
    private final FatoPermanenciaRepository fatos;
    private final ContextoCobrancaService contextos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final Clock clock;

    public MarcoFinanceiroAvariaService(
            MarcoFinanceiroAvariaRepository marcos,
            AvariaEstoqueRepository avarias,
            FatoPermanenciaRepository fatos,
            ContextoCobrancaService contextos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            Clock clock) {
        this.marcos = marcos;
        this.avarias = avarias;
        this.fatos = fatos;
        this.contextos = contextos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.clock = clock;
    }

    public List<FatoServicoDto.MarcoResposta> listar(@NotNull @Positive Long id) {
        var escopo = avarias.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        contextos.autorizar(escopo.getClienteId(), escopo.getArmazemId());
        return marcos.findByAvariaIdOrderByIdAsc(id).stream()
                .map(MarcoFinanceiroAvariaService::resposta)
                .toList();
    }

    @Transactional
    public FatoServicoDto.MarcoResposta registrar(
            @NotNull @Positive Long id, @NotNull @Valid FatoServicoDto.Marco d) {
        acesso.exigirGestor();
        var escopo = avarias.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var ctx = contextos.bloquear(escopo.getClienteId(), escopo.getArmazemId());
        String hash = operacoes.hash("MARCO_AVARIA", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, FatoServicoDto.MarcoResposta.class);
        if (replay != null) return replay;
        var a = avarias.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var f =
                fatos.findById(d.fatoPermanenciaId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (a.getReconhecidaEm() == null
                || !f.getUnidade().getId().equals(a.getUnidade().getId())
                || !List.of("RETIRADA", "AJUSTE_ESTOQUE").contains(f.getTipo())
                || f.getOcorridaEm().isBefore(a.getOcorridaEm())
                || a.getResolvidaEm() != null && !f.getOcorridaEm().isBefore(a.getResolvidaEm())
                || d.quantidadeAfetada().compareTo(f.getQuantidadeDepois()) > 0
                || d.quantidadeAfetada().compareTo(a.getQuantidade()) > 0)
            throw RegraNegocioException.conflito(
                    "MARCO_AVARIA_INVALIDO",
                    "Identifique o evento da mesma unidade dentro do período do dano e a parte afetada comprovadamente remanescente.");
        if (marcos.existsByAvariaIdAndFatoPermanenciaId(id, f.getId()))
            throw RegraNegocioException.conflito(
                    "MARCO_AVARIA_DUPLICADO", "Este marco já foi validado; preserve o histórico.");
        conferirContinuidade(a, f, d.quantidadeAfetada());
        var m =
                marcos.saveAndFlush(
                        new MarcoFinanceiroAvaria(
                                a,
                                f,
                                d.quantidadeAfetada(),
                                f.getQuantidadeDepois(),
                                f.getEquivalenciaDepois(),
                                acesso.usuario(),
                                Instant.now(clock)
                                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                                CadastroSupport.motivo(d.motivo())));
        var r = resposta(m);
        auditoria.registrar(
                "AVARIA_FINANCEIRA",
                id,
                "MARCO_AVARIA",
                CadastroSupport.motivo(d.motivo()),
                null,
                r);
        operacoes.salvar(
                d.operacaoId(), "MARCO_AVARIA", ctx.cliente(), ctx.armazem(), m.getId(), hash, r);
        return r;
    }

    private void conferirContinuidade(
            br.com.rodogarcia.wms.models.AvariaEstoque a,
            br.com.rodogarcia.wms.models.FatoPermanencia novo,
            java.math.BigDecimal afetada) {
        var conhecidos = new java.util.HashMap<Long, java.math.BigDecimal>();
        for (var m : marcos.findByAvariaIdOrderByIdAsc(a.getId()))
            conhecidos.put(m.getFatoPermanencia().getId(), m.getQuantidadeAfetada());
        conhecidos.put(novo.getId(), afetada);
        var minimo = a.getQuantidade();
        var maximo = a.getQuantidade();
        var quantidade = a.getQuantidadeBase();
        for (var evento : fatos.findByUnidadeIdOrderByOcorridaEmAscIdAsc(a.getUnidade().getId())) {
            if (!List.of("RETIRADA", "AJUSTE_ESTOQUE").contains(evento.getTipo())
                    || evento.getOcorridaEm().isBefore(a.getOcorridaEm())
                    || a.getResolvidaEm() != null
                            && !evento.getOcorridaEm().isBefore(a.getResolvidaEm())) continue;
            if (quantidade.compareTo(evento.getQuantidadeAntes()) != 0) throw continuidade();
            var removida =
                    evento.getQuantidadeAntes()
                            .subtract(evento.getQuantidadeDepois())
                            .max(java.math.BigDecimal.ZERO);
            minimo = minimo.subtract(removida).max(java.math.BigDecimal.ZERO);
            maximo = maximo.min(evento.getQuantidadeDepois());
            quantidade = evento.getQuantidadeDepois();
            var validada = conhecidos.get(evento.getId());
            if (validada != null) {
                if (validada.compareTo(minimo) < 0 || validada.compareTo(maximo) > 0)
                    throw continuidade();
                minimo = validada;
                maximo = validada;
            }
        }
    }

    private static RegraNegocioException continuidade() {
        return RegraNegocioException.conflito(
                "CONTINUIDADE_AVARIA_INVALIDA",
                "Marcos fora da continuidade física/cronológica do mesmo dano; quantidade afetada não pode reaparecer nem diminuir além da remoção comprovada.");
    }

    private static FatoServicoDto.MarcoResposta resposta(MarcoFinanceiroAvaria m) {
        return new FatoServicoDto.MarcoResposta(
                m.getId(),
                m.getAvaria().getId(),
                m.getFatoPermanencia().getId(),
                m.getQuantidadeAfetada(),
                m.getQuantidadeBase(),
                m.getEquivalenciaBase(),
                m.getValidadoEm(),
                m.getUsuario(),
                m.getMotivo());
    }
}
