package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.DocumentoSaida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.DevolucaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Vínculo fiscal da entrada ao fato operacional; não cria quantidade de estoque. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalRegularizacaoService {
    private final FiscalNfeRepository repository;
    private final DocumentoSaidaRepository documentos;
    private final BaixaSaidaRepository baixas;
    private final DevolucaoSaidaRepository devolucoes;
    private final JsonMapper mapper;
    private final AcessoService acesso;
    private final Clock clock;

    public FiscalRegularizacaoService(
            FiscalNfeRepository repository,
            DocumentoSaidaRepository documentos,
            BaixaSaidaRepository baixas,
            DevolucaoSaidaRepository devolucoes,
            JsonMapper mapper,
            AcessoService acesso,
            Clock clock) {
        this.repository = repository;
        this.documentos = documentos;
        this.baixas = baixas;
        this.devolucoes = devolucoes;
        this.mapper = mapper;
        this.acesso = acesso;
        this.clock = clock;
    }

    public void conferirRetorno(PedidoSaida pedido, List<String> ids) {
        if (repository.disponivel()
                && repository.doPedido(pedido.getId()).stream()
                        .anyMatch(
                                s ->
                                        !s.natureza().equals("REGULARIZACAO_ENTRADA")
                                                && !java.util.Set.of("AUTORIZADA", "CANCELADA")
                                                        .contains(s.situacao())))
            throw RegraNegocioException.conflito(
                    "PENDENCIA_FISCAL",
                    "Resolva o resultado das solicitações fiscais antes de reendereçar a mercadoria.");
        var ativos =
                documentos.findByPedidoIdOrderById(pedido.getId()).stream()
                        .filter(d -> d.getSituacao().equals("AUTORIZADO"))
                        .map(DocumentoSaida::getId)
                        .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            if (!ativos.isEmpty())
                throw RegraNegocioException.conflito(
                        "DOCUMENTO_ATIVO",
                        "Cancele os documentos quando permitido ou vincule as novas notas de entrada autorizadas antes de reendereçar.");
            return;
        }
        if (Set.copyOf(ids).size() != ids.size())
            throw CadastroSupport.invalido("Regularização fiscal repetida.");
        var origens =
                ids.stream()
                        .map(id -> entrada(pedido, id).documentoOrigemId())
                        .collect(Collectors.toSet());
        if (!origens.equals(ativos))
            throw CadastroSupport.invalido(
                    "As notas de entrada devem regularizar todos e somente os documentos ativos do retorno.");
    }

    public void conferirDevolucao(PedidoSaida pedido, ExpedicaoDto.Devolver comando) {
        if (comando.solicitacaoEntradaId() == null) return;
        var s = entrada(pedido, comando.solicitacaoEntradaId());
        var n = comando.nota();
        if (!s.emitenteCnpj().equals(n.emitenteCnpj())
                || !s.numero().equals(Long.toString(Long.parseLong(n.numero())))
                || !s.serie().equals(Integer.toString(Integer.parseInt(n.serie())))
                || !s.chaveAcesso().equals(n.chaveAcesso()))
            throw CadastroSupport.invalido(
                    "A nota informada não corresponde à entrada fiscal autorizada.");
        var snapshot = mapper.readValue(s.documentoJson(), FiscalNfeDto.DocumentoPlanejado.class);
        var esperado = new HashMap<String, BigDecimal>();
        var recebido = new HashMap<String, BigDecimal>();
        for (var i : snapshot.itens())
            esperado.merge(
                    i.reservaId() + ":" + i.entradaOrigemId(), i.quantidade(), BigDecimal::add);
        var originais = baixas.buscarDoPedido(pedido.getId());
        for (var i : comando.itens()) {
            var baixa =
                    originais.stream()
                            .filter(b -> b.getId().equals(i.baixaId()))
                            .findFirst()
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            recebido.merge(
                    baixa.getReserva().getId() + ":" + baixa.getEntradaOrigem().getId(),
                    i.quantidade(),
                    BigDecimal::add);
        }
        if (!esperado.keySet().equals(recebido.keySet())
                || esperado.entrySet().stream()
                        .anyMatch(e -> e.getValue().compareTo(recebido.get(e.getKey())) != 0))
            throw CadastroSupport.invalido(
                    "Confira as quantidades efetivamente retornadas; elas devem corresponder à nova nota de entrada vinculada.");
    }

    public void concluir(PedidoSaida pedido, List<String> ids, UUID operacao, Long pedidoEntrada) {
        for (var id : ids) {
            var s = entrada(pedido, id);
            if (pedidoEntrada != null) {
                var snap =
                        mapper.readValue(s.documentoJson(), FiscalNfeDto.DocumentoPlanejado.class);
                var novas =
                        devolucoes.buscarDoPedido(pedido.getId()).stream()
                                .filter(d -> d.getPedidoEntrada().getId().equals(pedidoEntrada))
                                .toList();
                for (var nova : novas) {
                    var baixa = nova.getBaixa();
                    var origem =
                            snap.itens().stream()
                                    .filter(
                                            i ->
                                                    i.reservaId().equals(baixa.getReserva().getId())
                                                            && i.entradaOrigemId()
                                                                    .equals(
                                                                            baixa.getEntradaOrigem()
                                                                                    .getId()))
                                    .findFirst()
                                    .orElseThrow();
                    if (nova.getQuantidade().compareTo(origem.quantidade()) != 0)
                        throw CadastroSupport.invalido(
                                "Valor da nova entrada exige a mesma parcela de origem conferida.");
                    nova.getEntradaNova()
                            .getItemChegada()
                            .getItemNota()
                            .complementarValor(origem.valorTotal());
                }
            }
            repository.concluirRegularizacao(
                    id, operacao.toString(), pedidoEntrada, clock.instant());
            repository.evento(
                    id,
                    "REGULARIZACAO_FISICA",
                    acesso.usuario(),
                    pedidoEntrada == null
                            ? "Reendereçamento da mercadoria já presente, sem duplicar estoque."
                            : "Retorno recebido em nova entrada vinculada à saída original.",
                    clock.instant());
        }
    }

    public void conferirRetirada(Long pedido) {
        if (!repository.disponivel()) return;
        if (repository.doPedido(pedido).stream()
                .anyMatch(
                        s ->
                                s.natureza().equals("REGULARIZACAO_ENTRADA")
                                        || !s.situacao().equals("AUTORIZADA")
                                                && !s.situacao().equals("CANCELADA")))
            throw RegraNegocioException.conflito(
                    "PENDENCIA_FISCAL",
                    "Resolva as solicitações fiscais e a regularização antes da retirada.");
    }

    public void impedirCancelamentoLocal(Long pedido, Long documento) {
        if (repository.disponivel()
                && repository.doPedido(pedido).stream()
                        .anyMatch(s -> documento.equals(s.documentoId())))
            throw RegraNegocioException.conflito(
                    "CANCELAMENTO_INTEGRADO",
                    "Esta nota foi emitida pela integração. Solicite e consulte o cancelamento fiscal no acompanhamento da NF-e.");
    }

    private SolicitacaoNfe entrada(PedidoSaida pedido, String id) {
        if (!repository.disponivel())
            throw CadastroSupport.invalido("Integração fiscal indisponível.");
        acesso.cliente(pedido.getCliente().getId());
        acesso.armazem(pedido.getArmazem().getId());
        var s = repository.consultar(id, true);
        if (s == null
                || s.pedidoId() != pedido.getId()
                || !s.natureza().equals("REGULARIZACAO_ENTRADA")
                || !s.situacao().equals("AUTORIZADA")
                || repository.regularizada(id))
            throw CadastroSupport.invalido(
                    "A nota de entrada deve estar autorizada, pertencer ao pedido e ainda aguardar o processamento físico.");
        return s;
    }
}
