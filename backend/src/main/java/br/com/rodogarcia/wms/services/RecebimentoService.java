package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.repositories.ChegadaRecebimentoRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.ItemChegadaRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class RecebimentoService {
    private final PedidoEntradaService pedidos;
    private final ChegadaRecebimentoRepository chegadas;
    private final ItemChegadaRepository itens;
    private final ItemNotaEntradaRepository previstos;
    private final EntradaConferidaRepository entradas;
    private final AcessoService acesso;
    private final Clock clock;
    private final JsonMapper mapper;
    private final OperacaoAdministrativaService operacoes;

    public RecebimentoService(
            PedidoEntradaService pedidos,
            ChegadaRecebimentoRepository chegadas,
            ItemChegadaRepository itens,
            ItemNotaEntradaRepository previstos,
            EntradaConferidaRepository entradas,
            AcessoService acesso,
            Clock clock,
            JsonMapper mapper,
            OperacaoAdministrativaService operacoes) {
        this.operacoes = operacoes;
        this.pedidos = pedidos;
        this.chegadas = chegadas;
        this.itens = itens;
        this.previstos = previstos;
        this.entradas = entradas;
        this.acesso = acesso;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Transactional
    public PedidoEntradaDto.Resumo registrarChegada(
            @NotNull @Positive Long id, @NotNull @Valid RecebimentoDto.RegistrarChegada dados) {
        var p = pedidos.bloquear(id);
        String hash = NfeXmlService.hash(mapper.writeValueAsString(dados));
        var anterior = chegadas.findByPedidoIdAndOperacaoId(id, dados.operacaoId().toString());
        if (anterior.isPresent()) {
            if (!hash.equals(anterior.get().getConteudoHash()))
                throw RegraNegocioException.conflito(
                        "OPERACAO_REUTILIZADA", "Identificador já utilizado com outros dados.");
            return PedidoEntradaDto.Resumo.de(p);
        }
        PedidoEntradaService.versao(p, dados.versao());
        PedidoEntradaService.exigirSituacao(
                p, SituacaoPedidoEntrada.EM_CONFERENCIA, SituacaoPedidoEntrada.QUARENTENA);
        pedidos.validarVinculos(p);
        if (dados.chegouEm().isAfter(Instant.now(clock))
                || dados.chegouEm().isBefore(Instant.parse("1900-01-01T00:00:00Z")))
            throw CadastroSupport.invalido("Chegada deve ser um instante passado válido.");
        if (chegadas.countByPedidoId(id) >= 100)
            throw CadastroSupport.invalido(
                    "Limite de 100 lançamentos de chegada por pedido excedido.");
        var porId = new HashMap<Long, ItemNotaEntrada>();
        previstos.buscarDoPedido(id).forEach(i -> porId.put(i.getId(), i));
        var combinacoes = new HashSet<List<Object>>();
        var antes = PedidoEntradaDto.Resumo.de(p);
        var chegada =
                chegadas.saveAndFlush(
                        new ChegadaRecebimento(
                                p,
                                dados.operacaoId().toString(),
                                hash,
                                dados.chegouEm().truncatedTo(ChronoUnit.MICROS),
                                agora(),
                                acesso.usuario(),
                                CadastroSupport.motivo(dados.observacao())));
        int seq = 0;
        for (var item : dados.itens()) {
            var origem = porId.get(item.itemNotaId());
            if (origem == null)
                throw CadastroSupport.invalido("Item não pertence às notas deste pedido.");
            var produto = origem.getProduto();
            String lote = item.lote() == null ? null : item.lote().strip();
            if (lote != null && lote.isEmpty())
                throw CadastroSupport.invalido("Lote informado não pode ser vazio.");
            if (produto.getControlaLote() && lote == null)
                throw CadastroSupport.invalido("Produto exige lote na conferência.");
            if (produto.getControlaValidade() && item.validade() == null)
                throw CadastroSupport.invalido("Produto exige validade na conferência.");
            if (!combinacoes.add(
                    List.of(
                            item.itemNotaId(),
                            lote == null ? "" : lote,
                            item.validade() == null ? "" : item.validade())))
                throw CadastroSupport.invalido("Item/lote/validade repetidos na mesma chegada.");
            CadastroSupport.quantidade(
                    produto, item.quantidadeBoa().add(item.quantidadeAvariada()));
            if (item.quantidadeBoa().stripTrailingZeros().scale() > produto.getPrecisaoQuantidade()
                    || item.quantidadeAvariada().stripTrailingZeros().scale()
                            > produto.getPrecisaoQuantidade())
                throw CadastroSupport.invalido("Precisão incompatível com o produto.");
            itens.save(
                    new ItemChegada(
                            chegada,
                            ++seq,
                            origem,
                            lote,
                            item.validade(),
                            item.quantidadeBoa(),
                            item.quantidadeAvariada()));
        }
        itens.flush();
        for (var quantidade : itens.resumir(id)) {
            BigDecimal total =
                    quantidade.getBoa().add(quantidade.getAvariada()).stripTrailingZeros();
            if (total.precision() - total.scale() > 13)
                throw CadastroSupport.invalido("Quantidade acumulada excede o limite do item.");
        }
        if (pedidos.detalhe(p).divergente()) p.atualizar(SituacaoPedidoEntrada.QUARENTENA, agora());
        return pedidos.registrar(
                p,
                antes,
                "CHEGADA_REGISTRADA",
                dados.observacao(),
                Map.of("chegadaId", chegada.getId(), "dados", dados));
    }

    @Transactional
    public PedidoEntradaDto.Resumo estornar(
            @NotNull @Positive Long id,
            @NotNull @Positive Long chegadaId,
            @NotNull @Valid RevisaoCadastroRequest dados) {
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        PedidoEntradaService.versao(p, dados.versao());
        PedidoEntradaService.exigirSituacao(
                p, SituacaoPedidoEntrada.EM_CONFERENCIA, SituacaoPedidoEntrada.QUARENTENA);
        var chegada =
                chegadas.findByIdAndPedidoId(chegadaId, id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (chegada.isEstornada())
            throw RegraNegocioException.conflito("CHEGADA_ESTORNADA", "Chegada já estornada.");
        var antes = PedidoEntradaDto.Resumo.de(p);
        chegada.estornar(agora(), acesso.usuario(), CadastroSupport.motivo(dados.motivo()));
        p.atualizar(SituacaoPedidoEntrada.QUARENTENA, agora());
        chegadas.flush();
        return pedidos.registrar(
                p, antes, "CHEGADA_ESTORNADA", dados.motivo(), Map.of("chegadaId", chegadaId));
    }

    @Transactional
    public PedidoEntradaDto.Resumo efetivar(
            @NotNull @Positive Long id, @NotNull @Valid PedidoEntradaDto.Efetivar dados) {
        return efetivarDatado(id, dados, null);
    }

    @Transactional
    public PedidoEntradaDto.Resumo efetivarContingencia(
            @NotNull @Positive Long id,
            @NotNull @Valid PedidoEntradaDto.Efetivar dados,
            @NotNull UUID operacao,
            @NotNull Instant ocorridaEm,
            @NotNull String identidadeFato) {
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        String hash =
                operacoes.hash(
                        "ENTRADA_CONTINGENCIA", id, List.of(dados, ocorridaEm, identidadeFato));
        if (p.getCliente().getSituacao() != br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO
                || p.getArmazem().getSituacao()
                        != br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO)
            acesso.exigirGestor();
        var replay = operacoes.repetida(operacao, hash, PedidoEntradaDto.Resumo.class);
        if (replay != null) return replay;
        var chegadasAtuais = chegadas.findByPedidoIdOrderByIdAsc(id);
        if (ocorridaEm.isAfter(agora())
                || chegadasAtuais.stream()
                        .filter(c -> !c.isEstornada())
                        .anyMatch(c -> c.getChegouEm().isAfter(ocorridaEm)))
            throw RegraNegocioException.conflito(
                    "HISTORICO_CONTINGENCIA_INSUFICIENTE",
                    "Instante de efetivação deve suceder todas as chegadas comprovadas, sem data futura.");
        var resposta = efetivarDatado(id, dados, ocorridaEm);
        operacoes.salvar(
                operacao,
                "ENTRADA_CONTINGENCIA",
                p.getCliente(),
                p.getArmazem(),
                id,
                hash,
                resposta);
        return resposta;
    }

    private PedidoEntradaDto.Resumo efetivarDatado(
            Long id, PedidoEntradaDto.Efetivar dados, Instant ocorridaEm) {
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        PedidoEntradaService.versao(p, dados.versao());
        PedidoEntradaService.exigirSituacao(
                p, SituacaoPedidoEntrada.EM_CONFERENCIA, SituacaoPedidoEntrada.QUARENTENA);
        pedidos.validarVinculos(p);
        var detalhe = pedidos.detalhe(p);
        if (detalhe.divergente() && !dados.aceitarDivergencias())
            throw RegraNegocioException.conflito(
                    "DIVERGENCIA_PENDENTE",
                    "Registre a solução com o cliente e aceite explicitamente as diferenças físicas.");
        var recebidos = itens.buscarAtivos(id);
        if (recebidos.isEmpty())
            throw CadastroSupport.invalido("Não há quantidade física para efetivar.");
        Map<Long, Instant> fifo = new HashMap<>();
        detalhe.notas()
                .forEach(
                        n -> {
                            if (n.primeiraChegada() != null) fifo.put(n.id(), n.primeiraChegada());
                        });
        var antes = PedidoEntradaDto.Resumo.de(p);
        Instant agora = agora();
        for (var recebido : recebidos)
            entradas.save(
                    new EntradaConferida(
                            recebido,
                            fifo.get(recebido.getItemNota().getNota().getId()),
                            ocorridaEm == null ? agora : ocorridaEm.truncatedTo(ChronoUnit.MICROS),
                            recebido.getQuantidadeBoa(),
                            recebido.getQuantidadeAvariada()));
        entradas.flush();
        p.concluir(
                CadastroSupport.motivo(dados.motivo()),
                ocorridaEm == null ? agora : ocorridaEm.truncatedTo(ChronoUnit.MICROS),
                agora);
        return pedidos.registrar(
                p,
                antes,
                "ENTRADA_EFETIVADA",
                dados.motivo(),
                Map.of(
                        "aceitarDivergencias",
                        dados.aceitarDivergencias(),
                        "divergente",
                        detalhe.divergente(),
                        "registros",
                        recebidos.size()));
    }

    public PaginaResponse<RecebimentoDto.Chegada> listarChegadas(
            @NotNull @Positive Long id, int pagina, int tamanho) {
        pedidos.obter(id);
        var page = chegadas.findByPedidoId(id, CadastroSupport.pagina(pagina, tamanho));
        var ids = page.getContent().stream().map(ChegadaRecebimento::getId).toList();
        var porChegada =
                ids.isEmpty()
                        ? Map.<Long, List<ItemChegada>>of()
                        : itens.buscarDasChegadas(ids).stream()
                                .collect(Collectors.groupingBy(i -> i.getChegada().getId()));
        return PaginaResponse.de(
                page,
                c ->
                        RecebimentoDto.Chegada.de(
                                c,
                                porChegada.getOrDefault(c.getId(), List.of()).stream()
                                        .map(RecebimentoDto.ItemFisico::de)
                                        .toList()));
    }

    public PaginaResponse<RecebimentoDto.Entrada> listarEntradas(
            @NotNull @Positive Long id, int pagina, int tamanho) {
        pedidos.obter(id);
        return PaginaResponse.de(
                entradas.findByItemChegadaChegadaPedidoId(
                        id, CadastroSupport.pagina(pagina, tamanho)),
                RecebimentoDto.Entrada::de);
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
