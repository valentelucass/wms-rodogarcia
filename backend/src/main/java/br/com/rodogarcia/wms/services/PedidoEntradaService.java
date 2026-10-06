package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ChegadaRecebimentoRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ItemChegadaRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class PedidoEntradaService {
    private final PedidoEntradaRepository pedidos;
    private final NotaEntradaRepository notas;
    private final ItemNotaEntradaRepository itens;
    private final ItemChegadaRepository fisicos;
    private final ChegadaRecebimentoRepository chegadas;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final Clock clock;

    public PedidoEntradaService(
            PedidoEntradaRepository pedidos,
            NotaEntradaRepository notas,
            ItemNotaEntradaRepository itens,
            ItemChegadaRepository fisicos,
            ChegadaRecebimentoRepository chegadas,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            AcessoService acesso,
            AuditoriaService auditoria,
            Clock clock) {
        this.pedidos = pedidos;
        this.notas = notas;
        this.itens = itens;
        this.fisicos = fisicos;
        this.chegadas = chegadas;
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.clock = clock;
    }

    @Transactional
    public PedidoEntradaDto.Resumo criar(@NotNull @Valid PedidoEntradaDto.Criar dados) {
        acesso.cliente(dados.clienteId());
        acesso.armazem(dados.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(dados.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(cliente);
        CadastroSupport.ativo(armazem);
        var p =
                pedidos.saveAndFlush(
                        new PedidoEntrada(
                                cliente,
                                armazem,
                                CadastroSupport.codigo(dados.referencia()),
                                agora()));
        var resposta = PedidoEntradaDto.Resumo.de(p);
        auditoria.registrar(
                "PEDIDO_ENTRADA", p.getId(), "CRIACAO", "Pedido de entrada criado", null, resposta);
        return resposta;
    }

    public PaginaResponse<PedidoEntradaDto.Resumo> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
        return PaginaResponse.de(
                pedidos.findByClienteIdAndArmazemId(
                        clienteId, armazemId, CadastroSupport.pagina(pagina, tamanho)),
                PedidoEntradaDto.Resumo::de);
    }

    public PedidoEntradaDto.Detalhe consultar(@NotNull @Positive Long id) {
        return detalhe(obter(id));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PedidoEntrada bloquear(Long id) {
        var p = pedidos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(p);
        return p;
    }

    public PedidoEntrada obter(Long id) {
        var p = pedidos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(p);
        return p;
    }

    private void autorizar(PedidoEntrada p) {
        acesso.cliente(p.getCliente().getId());
        acesso.armazem(p.getArmazem().getId());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validarVinculos(PedidoEntrada p) {
        // Ordem global: pedido, cliente, armazem e produtos por ID. IDs projetados evitam estado
        // JPA antigo antes do lock.
        CadastroSupport.ativo(
                clientes.buscarParaAtualizar(p.getCliente().getId())
                        .orElseThrow(RegraNegocioException::naoEncontrado));
        CadastroSupport.ativo(
                armazens.buscarParaAtualizar(p.getArmazem().getId())
                        .orElseThrow(RegraNegocioException::naoEncontrado));
        for (Long id : itens.buscarProdutosIds(p.getId())) {
            CadastroSupport.ativo(
                    produtos.buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado));
        }
    }

    public static void versao(PedidoEntrada p, long versao) {
        if (p.getVersao() != versao)
            throw RegraNegocioException.conflito(
                    "VERSAO_DESATUALIZADA",
                    "Pedido alterado. Consulte novamente antes de confirmar.");
    }

    public static void exigirSituacao(PedidoEntrada p, SituacaoPedidoEntrada... permitidas) {
        if (!List.of(permitidas).contains(p.getSituacao()))
            throw RegraNegocioException.conflito(
                    "SITUACAO_INVALIDA", "A situação do pedido não permite esta operação.");
    }

    @Transactional
    public PedidoEntradaDto.Resumo iniciar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        var p = bloquear(id);
        versao(p, dados.versao());
        exigirSituacao(p, SituacaoPedidoEntrada.RASCUNHO);
        validarVinculos(p);
        if (notas.countByPedidoId(id) == 0)
            throw CadastroSupport.invalido("Inclua ao menos uma nota antes de conferir.");
        var antes = PedidoEntradaDto.Resumo.de(p);
        p.atualizar(SituacaoPedidoEntrada.EM_CONFERENCIA, agora());
        return registrar(p, antes, "CONFERENCIA_INICIADA", dados.motivo(), Map.of());
    }

    @Transactional
    public PedidoEntradaDto.Resumo cancelar(
            @NotNull @Positive Long id, @NotNull @Valid RevisaoCadastroRequest dados) {
        acesso.exigirSupervisor();
        var p = bloquear(id);
        versao(p, dados.versao());
        exigirSituacao(p, SituacaoPedidoEntrada.RASCUNHO, SituacaoPedidoEntrada.EM_CONFERENCIA);
        if (chegadas.countByPedidoId(id) > 0)
            throw RegraNegocioException.conflito(
                    "CHEGADA_EXISTENTE",
                    "Pedido com histórico físico exige tratamento operacional; não pode ser cancelado por esta ação.");
        var antes = PedidoEntradaDto.Resumo.de(p);
        p.atualizar(SituacaoPedidoEntrada.CANCELADO, agora());
        return registrar(p, antes, "PEDIDO_CANCELADO", dados.motivo(), Map.of());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public PedidoEntradaDto.Resumo registrar(
            PedidoEntrada p,
            PedidoEntradaDto.Resumo antes,
            String acao,
            String motivo,
            Object evento) {
        p.atualizar(p.getSituacao(), agora());
        pedidos.flush();
        var depois = PedidoEntradaDto.Resumo.de(p);
        auditoria.registrar(
                "PEDIDO_ENTRADA",
                p.getId(),
                acao,
                CadastroSupport.motivo(motivo),
                antes,
                Map.of("pedido", depois, "evento", evento));
        return depois;
    }

    public PedidoEntradaDto.Detalhe detalhe(PedidoEntrada p) {
        var recebidas = new HashMap<Long, ItemChegadaRepository.QuantidadeRecebida>();
        fisicos.resumir(p.getId()).forEach(q -> recebidas.put(q.getItemId(), q));
        var itensPedido = itens.buscarDoPedido(p.getId());
        Map<Long, Instant> fifo = new HashMap<>();
        for (var i : itensPedido) {
            var q = recebidas.get(i.getId());
            if (q != null)
                fifo.merge(
                        i.getNota().getId(),
                        q.getPrimeiraChegada(),
                        (a, b) -> a.isBefore(b) ? a : b);
        }
        var notasDto =
                notas.findByPedidoIdOrderById(p.getId()).stream()
                        .map(
                                n ->
                                        new PedidoEntradaDto.Nota(
                                                n.getId(),
                                                n.getEmitente(),
                                                n.getSerie(),
                                                n.getNumero(),
                                                n.getEmissao(),
                                                n.getChaveAcesso(),
                                                n.getXmlHash() != null,
                                                fifo.get(n.getId()),
                                                itensPedido.stream()
                                                        .filter(
                                                                i ->
                                                                        i.getNota()
                                                                                .getId()
                                                                                .equals(n.getId()))
                                                        .map(
                                                                i -> {
                                                                    var q =
                                                                            recebidas.get(
                                                                                    i.getId());
                                                                    var boa =
                                                                            q == null
                                                                                    ? BigDecimal
                                                                                            .ZERO
                                                                                    : q.getBoa();
                                                                    var avariada =
                                                                            q == null
                                                                                    ? BigDecimal
                                                                                            .ZERO
                                                                                    : q
                                                                                            .getAvariada();
                                                                    return new PedidoEntradaDto
                                                                            .ItemConferencia(
                                                                            i.getId(),
                                                                            i.getNumeroItem(),
                                                                            i.getProduto().getId(),
                                                                            i.getProduto().getSku(),
                                                                            i
                                                                                    .getQuantidadePrevista(),
                                                                            boa,
                                                                            avariada,
                                                                            boa.add(avariada)
                                                                                    .subtract(
                                                                                            i
                                                                                                    .getQuantidadePrevista()),
                                                                            i.getValorMercadoria());
                                                                })
                                                        .toList()))
                        .toList();
        boolean divergente =
                notasDto.stream()
                        .flatMap(n -> n.itens().stream())
                        .anyMatch(
                                i ->
                                        i.diferenca().signum() != 0
                                                || i.recebidaAvariada().signum() > 0);
        return new PedidoEntradaDto.Detalhe(PedidoEntradaDto.Resumo.de(p), divergente, notasDto);
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
