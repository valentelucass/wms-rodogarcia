package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.FechamentoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.RetiradaSaidaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Identifica o compromisso histórico; não reativa cadastro nem autoriza nova execução física. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
@Transactional(propagation = Propagation.MANDATORY)
public class ResolucaoFinanceiraCadastroService {
    private final PedidoEntradaRepository entradas;
    private final PedidoSaidaRepository saidas;
    private final FechamentoCobrancaRepository fechamentos;
    private final EntradaConferidaRepository conferidas;
    private final RetiradaSaidaRepository retiradas;
    private final AcessoService acesso;

    public ResolucaoFinanceiraCadastroService(
            PedidoEntradaRepository entradas,
            PedidoSaidaRepository saidas,
            FechamentoCobrancaRepository fechamentos,
            EntradaConferidaRepository conferidas,
            RetiradaSaidaRepository retiradas,
            AcessoService acesso) {
        this.entradas = entradas;
        this.saidas = saidas;
        this.fechamentos = fechamentos;
        this.conferidas = conferidas;
        this.retiradas = retiradas;
        this.acesso = acesso;
    }

    public void conferir(ContextoCobrancaService.Contexto ctx, FechamentoCobrancaDto.Resolucao r) {
        if (r == null) {
            if (ctx.cliente().getSituacao() != SituacaoCadastro.ATIVO
                    || ctx.armazem().getSituacao() != SituacaoCadastro.ATIVO)
                throw RegraNegocioException.conflito(
                        "RESOLUCAO_FINANCEIRA_OBRIGATORIA",
                        "Gestor deve identificar o compromisso financeiro existente.");
            return;
        }
        acesso.exigirGestor();
        Long cliente, armazem;
        switch (r.tipo()) {
            case "PEDIDO_ENTRADA" -> {
                var p =
                        entradas.findById(r.compromissoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                cliente = p.getCliente().getId();
                armazem = p.getArmazem().getId();
                if (conferidas.countByItemChegadaChegadaPedidoId(p.getId()) == 0)
                    throw RegraNegocioException.conflito(
                            "COMPROMISSO_NAO_EXECUTADO",
                            "Entrada sem conferência executada não comprova histórico financeiro.");
            }
            case "PEDIDO_SAIDA" -> {
                var p =
                        saidas.findById(r.compromissoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                cliente = p.getCliente().getId();
                armazem = p.getArmazem().getId();
                if (retiradas.findByPedidoId(p.getId()).isEmpty())
                    throw RegraNegocioException.conflito(
                            "COMPROMISSO_NAO_EXECUTADO",
                            "Saída sem retirada executada não comprova histórico financeiro.");
            }
            case "FECHAMENTO" -> {
                var f =
                        fechamentos
                                .findById(r.compromissoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                cliente = f.getCliente().getId();
                armazem = f.getArmazem().getId();
            }
            default -> throw CadastroSupport.invalido("Tipo de compromisso financeiro inválido.");
        }
        if (!cliente.equals(ctx.cliente().getId()) || !armazem.equals(ctx.armazem().getId()))
            throw RegraNegocioException.conflito(
                    "CONTEXTO_DIVERGENTE",
                    "Compromisso não pertence ao cliente/armazém informado.");
    }

    public void conferirPeriodo(
            FechamentoCobrancaDto.Resolucao r,
            java.time.LocalDate inicio,
            java.time.LocalDate fim,
            java.time.Clock clock,
            String fuso) {
        if (r == null) return;
        if (fim.isAfter(
                java.time.Instant.now(clock)
                        .atZone(java.time.ZoneId.of(fuso == null ? "UTC" : fuso))
                        .toLocalDate()))
            throw RegraNegocioException.conflito(
                    "RESOLUCAO_NAO_HISTORICA",
                    "Resolução financeira identificada não fecha período futuro.");
        if (r.tipo().equals("FECHAMENTO")) {
            var f =
                    fechamentos
                            .findById(r.compromissoId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!inicio.equals(f.getPeriodoInicio()) || !fim.equals(f.getPeriodoFim()))
                throw RegraNegocioException.conflito(
                        "CONTEXTO_DIVERGENTE",
                        "Referência do fechamento precisa comprovar o período financeiro indicado.");
        }
    }

    public void conferirExecucao(
            FechamentoCobrancaDto.Resolucao r,
            Long entradaId,
            Long saidaId,
            java.time.Instant instante,
            String fuso) {
        if (r == null) return;
        if (r.tipo().equals("PEDIDO_ENTRADA") && !r.compromissoId().equals(entradaId)
                || r.tipo().equals("PEDIDO_SAIDA") && !r.compromissoId().equals(saidaId))
            throw RegraNegocioException.conflito(
                    "CONTEXTO_DIVERGENTE", "Fato deve resolver a execução identificada no pedido.");
        if (r.tipo().equals("FECHAMENTO")) {
            var f =
                    fechamentos
                            .findById(r.compromissoId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            var dia = instante.atZone(java.time.ZoneId.of(f.getContrato().getFuso())).toLocalDate();
            if (dia.isBefore(f.getPeriodoInicio()) || !dia.isBefore(f.getPeriodoFim()))
                throw RegraNegocioException.conflito(
                        "CONTEXTO_DIVERGENTE",
                        "Execução não pertence ao período do compromisso indicado.");
        }
    }

    public Object paraAuditoria(FechamentoCobrancaDto.Resolucao r, Object resultado) {
        return paraAuditoria(r, resultado, null, null);
    }

    public Object paraAuditoria(
            FechamentoCobrancaDto.Resolucao r,
            Object resultado,
            java.time.LocalDate inicio,
            java.time.LocalDate fim) {
        if (r == null) return resultado;
        Long cliente, armazem;
        java.time.LocalDate compromissoInicio = null, compromissoFim = null;
        switch (r.tipo()) {
            case "FECHAMENTO" -> {
                var f =
                        fechamentos
                                .findById(r.compromissoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                cliente = f.getCliente().getId();
                armazem = f.getArmazem().getId();
                compromissoInicio = f.getPeriodoInicio();
                compromissoFim = f.getPeriodoFim();
            }
            case "PEDIDO_ENTRADA" -> {
                var p =
                        entradas.findById(r.compromissoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                cliente = p.getCliente().getId();
                armazem = p.getArmazem().getId();
            }
            case "PEDIDO_SAIDA" -> {
                var p =
                        saidas.findById(r.compromissoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                cliente = p.getCliente().getId();
                armazem = p.getArmazem().getId();
            }
            default -> throw CadastroSupport.invalido("Tipo de compromisso financeiro inválido.");
        }
        return new AuditoriaResolucao(
                resultado,
                new CompromissoAuditado(
                        r.tipo(),
                        r.compromissoId(),
                        cliente,
                        armazem,
                        compromissoInicio,
                        compromissoFim,
                        inicio,
                        fim));
    }

    public record AuditoriaResolucao(Object resultado, CompromissoAuditado resolucao) {}

    public record CompromissoAuditado(
            String tipo,
            Long compromissoId,
            Long clienteId,
            Long armazemId,
            java.time.LocalDate periodoInicio,
            java.time.LocalDate periodoFim,
            java.time.LocalDate periodoEfeitoInicio,
            java.time.LocalDate periodoEfeitoFim) {}
}
