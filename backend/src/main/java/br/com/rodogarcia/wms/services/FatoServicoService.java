package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.RateioFatoServico;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.RateioFatoServicoRepository;
import br.com.rodogarcia.wms.repositories.RetiradaSaidaRepository;
import br.com.rodogarcia.wms.repositories.ServicoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FatoServicoService {
    private final FatoServicoRepository fatos;
    private final RateioFatoServicoRepository rateios;
    private final ServicoCobrancaRepository servicos;
    private final UnidadeLogisticaRepository unidades;
    private final PedidoEntradaRepository entradas;
    private final PedidoSaidaRepository saidas;
    private final RetiradaSaidaRepository retiradas;
    private final BaixaSaidaRepository baixas;
    private final ConteudoUnidadeRepository conteudos;
    private final NotaEntradaRepository notas;
    private final ProdutoRepository produtos;
    private final ContextoCobrancaService contextos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final LinhaTemporalEstoqueService temporal;
    private final Clock clock;
    private final ResolucaoFinanceiraCadastroService resolucaoCadastro;

    public FatoServicoService(
            FatoServicoRepository fatos,
            RateioFatoServicoRepository rateios,
            ServicoCobrancaRepository servicos,
            UnidadeLogisticaRepository unidades,
            PedidoEntradaRepository entradas,
            PedidoSaidaRepository saidas,
            RetiradaSaidaRepository retiradas,
            BaixaSaidaRepository baixas,
            ConteudoUnidadeRepository conteudos,
            NotaEntradaRepository notas,
            ProdutoRepository produtos,
            ContextoCobrancaService contextos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            LinhaTemporalEstoqueService temporal,
            Clock clock,
            ResolucaoFinanceiraCadastroService resolucaoCadastro) {
        this.resolucaoCadastro = resolucaoCadastro;
        this.fatos = fatos;
        this.rateios = rateios;
        this.servicos = servicos;
        this.unidades = unidades;
        this.entradas = entradas;
        this.saidas = saidas;
        this.retiradas = retiradas;
        this.baixas = baixas;
        this.conteudos = conteudos;
        this.notas = notas;
        this.produtos = produtos;
        this.contextos = contextos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.temporal = temporal;
        this.clock = clock;
    }

    private record Execucao(
            String chave,
            Instant instante,
            BigDecimal quantidade,
            UnidadeLogistica unidade,
            PedidoEntrada entrada,
            PedidoSaida saida,
            Produto produto,
            Map<Long, BigDecimal> cotas) {}

    @Transactional
    public FatoServicoDto.Fato registrar(@NotNull @Valid FatoServicoDto.Registrar d) {
        acesso.exigirSupervisor();
        var ctx = contextos.bloquear(d.clienteId(), d.armazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        var s =
                servicos.buscarParaAtualizar(d.servicoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (!"ATIVO".equals(s.getSituacao())) {
            acesso.exigirGestor();
            if (d.resolucao() == null) throw conflito("RESOLUCAO_FINANCEIRA_OBRIGATORIA");
        }
        String hash = operacoes.hash("REGISTRO_SERVICO", null, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, FatoServicoDto.Fato.class);
        if (replay != null) return replay;
        if ("ARMAZENAGEM".equals(s.getTipo()))
            throw CadastroSupport.invalido(
                    "Armazenagem deriva da memória diária, sem fato manual duplicado.");
        var e = execucao(s, d);
        if (!"ATIVO".equals(s.getSituacao()) && e.instante().isAfter(s.getAlteradoEm()))
            throw conflito("RESOLUCAO_NAO_HISTORICA");
        resolucaoCadastro.conferirExecucao(
                d.resolucao(),
                e.entrada() == null ? null : e.entrada().getId(),
                e.saida() == null ? null : e.saida().getId(),
                e.instante(),
                null);
        if (fatos.findByClienteIdAndArmazemIdAndServicoIdAndChaveFato(
                        d.clienteId(), d.armazemId(), s.getId(), e.chave())
                .isPresent()) throw conflito("FATO_SERVICO_JA_CONFIRMADO");
        if ("PERCENTUAL".equals(s.getUnidade()) ? d.valorBase() == null : d.valorBase() != null)
            throw CadastroSupport.invalido(
                    "Base monetária explícita somente para serviço percentual.");
        Map<Long, BigDecimal> cotas = e.cotas();
        if (!d.cotas().isEmpty()) {
            if (!"ADICIONAL".equals(s.getTipo()))
                throw CadastroSupport.invalido(
                        "Entrada/saída usa rateio de origens, sem substituir cotas físicas.");
            cotas = new LinkedHashMap<>();
            for (var c : d.cotas()) {
                if (cotas.put(c.notaId(), c.cota()) != null)
                    throw conflito("NOTA_RATEIO_DUPLICADA");
                var n =
                        notas.findById(c.notaId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                if (e.entrada() == null || !n.getPedido().getId().equals(e.entrada().getId()))
                    throw conflito("NOTA_RATEIO_FORA_EXECUCAO");
            }
        }
        if (cotas.isEmpty()) throw conflito("RATEIO_INSUFICIENTE");
        var f =
                fatos.saveAndFlush(
                        new FatoServico(
                                ctx.cliente(),
                                ctx.armazem(),
                                s,
                                e.entrada(),
                                e.saida(),
                                e.unidade(),
                                e.produto(),
                                e.chave(),
                                d.origem(),
                                d.referenciaExecucao(),
                                e.instante(),
                                Instant.now(clock)
                                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                                e.quantidade(),
                                CadastroSupport.codigo(d.categoria()),
                                d.valorBase(),
                                CadastroSupport.texto(d.criterioRateio()),
                                "CONFIRMADO",
                                null,
                                acesso.usuario()));
        for (var c : cotas.entrySet())
            rateios.save(
                    new RateioFatoServico(f, notas.getReferenceById(c.getKey()), c.getValue()));
        rateios.flush();
        var resposta = resposta(f);
        auditoria.registrar(
                "FATO_SERVICO",
                f.getId(),
                "REGISTRO_SERVICO",
                CadastroSupport.motivo(d.motivo()),
                null,
                resposta);
        operacoes.salvar(
                d.operacaoId(),
                "REGISTRO_SERVICO",
                ctx.cliente(),
                ctx.armazem(),
                f.getId(),
                hash,
                resposta);
        return resposta;
    }

    private Execucao execucao(ServicoCobranca s, FatoServicoDto.Registrar d) {
        UnidadeLogistica u =
                d.unidadeId() == null
                        ? null
                        : unidades.findById(d.unidadeId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
        PedidoEntrada pe =
                d.pedidoEntradaId() == null
                        ? null
                        : entradas.findById(d.pedidoEntradaId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
        PedidoSaida ps =
                d.pedidoSaidaId() == null
                        ? null
                        : saidas.findById(d.pedidoSaidaId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
        Produto produto =
                d.produtoId() == null
                        ? null
                        : produtos.findById(d.produtoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
        if (u != null) {
            contexto(u.getPedido(), d);
            if (pe != null && !pe.getId().equals(u.getPedido().getId()))
                throw conflito("CONTEXTO_EXECUCAO_INVALIDO");
            pe = u.getPedido();
            if (produto != null && !produto.getId().equals(u.getProduto().getId()))
                throw conflito("CONTEXTO_EXECUCAO_INVALIDO");
            produto = u.getProduto();
        }
        if (pe != null) contexto(pe, d);
        if (ps != null
                && (!ps.getCliente().getId().equals(d.clienteId())
                        || !ps.getArmazem().getId().equals(d.armazemId())))
            throw conflito("CONTEXTO_EXECUCAO_INVALIDO");
        if (produto != null && !produto.getCliente().getId().equals(d.clienteId()))
            throw conflito("CONTEXTO_EXECUCAO_INVALIDO");
        var cotas = new HashMap<Long, BigDecimal>();
        Instant instante;
        BigDecimal quantidade;
        String chave;
        if ("ENTRADA".equals(s.getTipo())) {
            if (u == null
                    || ps != null
                    || u.getPrimeiroEnderecamentoEm() == null
                    || d.referenciaExecucao() != null) throw conflito("MARCO_ENTRADA_AUSENTE");
            instante = u.getPrimeiroEnderecamentoEm();
            var base = temporal.noInstante(u, instante);
            quantidade =
                    "UNIDADE_LOGISTICA".equals(s.getUnidade()) ? BigDecimal.ONE : base.quantidade();
            chave = "ENTRADA:UNIDADE:" + u.getId();
            for (var c : conteudos.buscarOrigens(u.getId())) {
                BigDecimal historica = c.getQuantidade();
                for (var b : baixas.buscarDaUnidade(u.getId()))
                    if (b.getEntradaOrigem().getId().equals(c.getEntrada().getId()))
                        historica = historica.add(b.getQuantidade());
                if (historica.signum() > 0)
                    cotas.merge(
                            c.getEntrada().getItemChegada().getItemNota().getNota().getId(),
                            historica,
                            BigDecimal::add);
            }
            if (cotas.values().stream()
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .compareTo(base.quantidade())
                    != 0) throw conflito("RATEIO_INSUFICIENTE");
        } else if ("SAIDA".equals(s.getTipo())) {
            if (ps == null || pe != null || u != null || d.referenciaExecucao() != null)
                throw conflito("MARCO_SAIDA_AUSENTE");
            var retirada =
                    retiradas
                            .findByPedidoId(ps.getId())
                            .orElseThrow(() -> conflito("MARCO_SAIDA_AUSENTE"));
            instante = retirada.getRetiradaEm();
            var bs = baixas.buscarDoPedido(ps.getId());
            if ("QUANTIDADE_PRODUTO".equals(s.getUnidade())) {
                if (produto == null) throw conflito("PRODUTO_EXECUCAO_NECESSARIO");
                Long pid = produto.getId();
                bs =
                        bs.stream()
                                .filter(
                                        b ->
                                                b.getReserva()
                                                        .getUnidade()
                                                        .getProduto()
                                                        .getId()
                                                        .equals(pid))
                                .toList();
                quantidade =
                        bs.stream()
                                .map(b -> b.getQuantidade())
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                for (var b : bs)
                    cotas.merge(
                            b.getEntradaOrigem().getItemChegada().getItemNota().getNota().getId(),
                            b.getQuantidade(),
                            BigDecimal::add);
                chave = "SAIDA:RETIRADA:" + retirada.getId() + ":SKU:" + pid;
            } else {
                if (produto != null)
                    throw CadastroSupport.invalido("Saída por unidade não é fragmentada por SKU.");
                var porUnidade =
                        bs.stream()
                                .collect(
                                        java.util.stream.Collectors.groupingBy(
                                                b -> b.getReserva().getUnidade().getId()));
                quantidade = BigDecimal.valueOf(porUnidade.size());
                for (var grupo : porUnidade.values()) {
                    BigDecimal soma =
                            grupo.stream()
                                    .map(b -> b.getQuantidade())
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    var notasGrupo = new java.util.TreeMap<Long, BigDecimal>();
                    for (var b : grupo)
                        notasGrupo.merge(
                                b.getEntradaOrigem()
                                        .getItemChegada()
                                        .getItemNota()
                                        .getNota()
                                        .getId(),
                                b.getQuantidade(),
                                BigDecimal::add);
                    BigDecimal restante = BigDecimal.ONE;
                    int i = 0;
                    for (var c : notasGrupo.entrySet()) {
                        BigDecimal cota =
                                ++i == notasGrupo.size()
                                        ? restante
                                        : c.getValue().divide(soma, 6, RoundingMode.DOWN);
                        restante = restante.subtract(cota);
                        if (cota.signum() > 0) cotas.merge(c.getKey(), cota, BigDecimal::add);
                    }
                }
                chave = "SAIDA:RETIRADA:" + retirada.getId();
            }
            if (quantidade.signum() <= 0) throw conflito("MARCO_SAIDA_AUSENTE");
        } else {
            if (pe == null
                    || d.referenciaExecucao() == null
                    || d.executadoEm() == null
                    || d.quantidade() == null) throw conflito("EXECUCAO_ADICIONAL_INSUFICIENTE");
            instante = d.executadoEm();
            if (instante.isBefore(pe.getCriadoEm())
                    || instante.isAfter(
                            Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS)))
                throw CadastroSupport.invalido("Execução exige instante comprovado não futuro.");
            quantidade = d.quantidade();
            chave =
                    "ADICIONAL:ENTRADA:"
                            + pe.getId()
                            + ":EXEC:"
                            + CadastroSupport.codigo(d.referenciaExecucao());

            if (d.cotas().isEmpty()) {
                if (u == null) throw conflito("RATEIO_INSUFICIENTE");
                for (var c : conteudos.buscarOrigens(u.getId()))
                    if (c.getQuantidade().signum() > 0)
                        cotas.merge(
                                c.getEntrada().getItemChegada().getItemNota().getNota().getId(),
                                c.getQuantidade(),
                                BigDecimal::add);
            }
        }
        if (!"ADICIONAL".equals(s.getTipo())
                && (d.executadoEm() != null && !d.executadoEm().equals(instante)
                        || d.quantidade() != null && d.quantidade().compareTo(quantidade) != 0))
            throw CadastroSupport.invalido(
                    "Instante/quantidade precisam corresponder ao fato operacional.");
        if (produto != null && "QUANTIDADE_PRODUTO".equals(s.getUnidade()))
            CadastroSupport.quantidade(produto, quantidade);
        return new Execucao(chave, instante, quantidade, u, pe, ps, produto, cotas);
    }

    private static void contexto(PedidoEntrada p, FatoServicoDto.Registrar d) {
        if (!p.getCliente().getId().equals(d.clienteId())
                || !p.getArmazem().getId().equals(d.armazemId()))
            throw conflito("CONTEXTO_EXECUCAO_INVALIDO");
    }

    public FatoServicoDto.Fato consultar(@NotNull @Positive Long id) {
        var escopo = fatos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        contextos.autorizar(escopo.getClienteId(), escopo.getArmazemId());
        return resposta(fatos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado));
    }

    public PaginaResponse<FatoServicoDto.Fato> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        contextos.autorizar(clienteId, armazemId);
        return PaginaResponse.de(
                fatos.findByClienteIdAndArmazemId(
                        clienteId, armazemId, CadastroSupport.pagina(pagina, tamanho)),
                this::resposta);
    }

    @Transactional
    public FatoServicoDto.Fato anular(
            @NotNull @Positive Long id, @NotNull @Valid FatoServicoDto.Anular d) {
        acesso.exigirGestor();
        var escopo = fatos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var ctx = contextos.bloquear(escopo.getClienteId(), escopo.getArmazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        var f = fatos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        resolucaoCadastro.conferirExecucao(
                d.resolucao(),
                f.getPedidoEntrada() == null ? null : f.getPedidoEntrada().getId(),
                f.getPedidoSaida() == null ? null : f.getPedidoSaida().getId(),
                f.getExecutadoEm(),
                null);
        String hash = operacoes.hash("ANULACAO_SERVICO", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, FatoServicoDto.Fato.class);
        if (replay != null) return replay;
        if (f.getVersao() != d.versao()) throw conflito("VERSAO_DESATUALIZADA");
        if (!f.getSituacao().equals("CONFIRMADO")) throw conflito("FATO_SERVICO_ANULADO");
        var antes = resposta(f);
        f.anular(Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        fatos.flush();
        var r = resposta(f);
        auditoria.registrar(
                "FATO_SERVICO",
                id,
                "ANULACAO_SERVICO",
                CadastroSupport.motivo(d.motivo()),
                antes,
                r);
        operacoes.salvar(
                d.operacaoId(), "ANULACAO_SERVICO", ctx.cliente(), ctx.armazem(), id, hash, r);
        return r;
    }

    @Transactional
    public PaginaResponse<FatoServicoDto.Sugestao> sugestoes(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull @Positive Long servicoId,
            int pagina,
            int tamanho) {
        contextos.bloquear(clienteId, armazemId);
        var s = servicos.findById(servicoId).orElseThrow(RegraNegocioException::naoEncontrado);
        if ("ENTRADA".equals(s.getTipo()))
            return PaginaResponse.de(
                    unidades.candidatasServicoEntrada(
                            clienteId,
                            armazemId,
                            servicoId,
                            CadastroSupport.pagina(pagina, tamanho)),
                    u -> {
                        BigDecimal q = null;
                        String pendencia = null;
                        try {
                            q =
                                    "UNIDADE_LOGISTICA".equals(s.getUnidade())
                                            ? BigDecimal.ONE
                                            : temporal.noInstante(u, u.getPrimeiroEnderecamentoEm())
                                                    .quantidade();
                        } catch (RegraNegocioException e) {
                            pendencia = "HISTORICO_INSUFICIENTE";
                        }
                        return new FatoServicoDto.Sugestao(
                                servicoId,
                                u.getId(),
                                null,
                                u.getProduto().getId(),
                                "ENTRADA:UNIDADE:" + u.getId(),
                                u.getPrimeiroEnderecamentoEm(),
                                q,
                                "",
                                pendencia);
                    });
        if ("SAIDA".equals(s.getTipo()) && "QUANTIDADE_PRODUTO".equals(s.getUnidade())) {
            var paginaValidada = CadastroSupport.pagina(pagina, tamanho);
            var paginacao =
                    org.springframework.data.domain.PageRequest.of(
                            paginaValidada.getPageNumber(),
                            paginaValidada.getPageSize(),
                            org.springframework.data.domain.Sort.by(
                                    "retirada.id", "reserva.unidade.produto.id"));
            return PaginaResponse.de(
                    baixas.candidatasQuantidade(clienteId, armazemId, servicoId, paginacao),
                    r ->
                            new FatoServicoDto.Sugestao(
                                    servicoId,
                                    null,
                                    r.getPedidoSaidaId(),
                                    r.getProdutoId(),
                                    "SAIDA:RETIRADA:"
                                            + r.getRetiradaId()
                                            + ":SKU:"
                                            + r.getProdutoId(),
                                    r.getExecutadoEm(),
                                    r.getQuantidade(),
                                    "",
                                    null));
        }
        if ("SAIDA".equals(s.getTipo()))
            return PaginaResponse.de(
                    retiradas.candidatasServico(
                            clienteId,
                            armazemId,
                            servicoId,
                            CadastroSupport.pagina(pagina, tamanho)),
                    r ->
                            new FatoServicoDto.Sugestao(
                                    servicoId,
                                    null,
                                    r.getPedido().getId(),
                                    null,
                                    "SAIDA:RETIRADA:" + r.getId(),
                                    r.getRetiradaEm(),
                                    "UNIDADE_LOGISTICA".equals(s.getUnidade())
                                            ? BigDecimal.valueOf(
                                                    baixas
                                                            .buscarDoPedido(r.getPedido().getId())
                                                            .stream()
                                                            .map(
                                                                    b ->
                                                                            b.getReserva()
                                                                                    .getUnidade()
                                                                                    .getId())
                                                            .distinct()
                                                            .count())
                                            : null,
                                    "",
                                    "QUANTIDADE_PRODUTO".equals(s.getUnidade())
                                            ? "SELECIONE_SKU"
                                            : ""));
        throw CadastroSupport.invalido(
                "Adicionais exigem execução identificada; armazenagem deriva da memória diária.");
    }

    private FatoServicoDto.Fato resposta(FatoServico f) {
        return new FatoServicoDto.Fato(
                f.getId(),
                f.getVersao(),
                f.getCliente().getId(),
                f.getArmazem().getId(),
                f.getServico().getId(),
                f.getChaveFato(),
                f.getOrigem(),
                f.getUnidade() == null ? null : f.getUnidade().getId(),
                f.getPedidoEntrada() == null ? null : f.getPedidoEntrada().getId(),
                f.getPedidoSaida() == null ? null : f.getPedidoSaida().getId(),
                f.getProduto() == null ? null : f.getProduto().getId(),
                f.getReferenciaExecucao(),
                f.getExecutadoEm(),
                f.getQuantidade(),
                f.getCategoria(),
                f.getValorBase(),
                f.getCriterioRateio(),
                f.getSituacao(),
                rateios.findByFatoIdOrderByNotaIdAsc(f.getId()).stream()
                        .map(r -> new FatoServicoDto.Cota(r.getNota().getId(), r.getCota()))
                        .toList());
    }

    private static RegraNegocioException conflito(String codigo) {
        return RegraNegocioException.conflito(
                codigo, "Execução, origem ou versão não comprovada para este serviço.");
    }
}
