package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.ChegadaRecebimentoRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.ItemChegadaRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Novo ciclo fiscal das parcelas presentes; quantidade, unidade e FIFO físicos são conservados. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class RegistroRegularizacaoInternaService {
    private final PedidoEntradaRepository pedidos;
    private final NotaEntradaRepository notas;
    private final ItemNotaEntradaRepository itens;
    private final ChegadaRecebimentoRepository chegadas;
    private final ItemChegadaRepository itensChegada;
    private final EntradaConferidaRepository entradas;
    private final ReservaSaidaRepository reservas;
    private final ConteudoUnidadeRepository conteudos;
    private final FiscalNfeXml xml;
    private final JsonMapper mapper;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;

    public RegistroRegularizacaoInternaService(
            PedidoEntradaRepository pedidos,
            NotaEntradaRepository notas,
            ItemNotaEntradaRepository itens,
            ChegadaRecebimentoRepository chegadas,
            ItemChegadaRepository itensChegada,
            EntradaConferidaRepository entradas,
            ReservaSaidaRepository reservas,
            ConteudoUnidadeRepository conteudos,
            FiscalNfeXml xml,
            JsonMapper mapper,
            AcessoService acesso,
            AuditoriaService auditoria) {
        this.pedidos = pedidos;
        this.notas = notas;
        this.itens = itens;
        this.chegadas = chegadas;
        this.itensChegada = itensChegada;
        this.entradas = entradas;
        this.reservas = reservas;
        this.conteudos = conteudos;
        this.xml = xml;
        this.mapper = mapper;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    private record Parcela(FiscalNfeDto.Item item, ReservaSaida reserva, ConteudoUnidade origem) {}

    private record Plano(
            SolicitacaoNfe solicitacao,
            FiscalNfeXml.Autorizacao autorizacao,
            List<Parcela> parcelas) {}

    @Transactional(propagation = Propagation.MANDATORY)
    public Long registrar(
            PedidoSaida saida, List<SolicitacaoNfe> documentos, UUID operacao, Instant agora) {
        acesso.exigirSupervisor();
        acesso.cliente(saida.getCliente().getId());
        acesso.armazem(saida.getArmazem().getId());
        if (documentos.isEmpty()) return null;
        var linhas = reservas.buscarDoPedido(saida.getId());
        var comprometidas = new HashMap<String, BigDecimal>();
        var planos = new ArrayList<Plano>();
        // Confere toda a composição antes de criar o novo ciclo. O chamador mantém os locks do
        // escopo.
        for (var documento : documentos) {
            if (documento.pedidoId() != saida.getId()
                    || !"REGULARIZACAO_ENTRADA".equals(documento.natureza())
                    || !"AUTORIZADA".equals(documento.situacao()))
                throw CadastroSupport.invalido("Entrada fiscal não corresponde à regularização.");
            var snapshot =
                    mapper.readValue(
                            documento.documentoJson(), FiscalNfeDto.DocumentoPlanejado.class);
            if (!"REGULARIZACAO_ENTRADA".equals(snapshot.natureza())
                    || !Objects.equals(snapshot.emitenteCnpj(), documento.emitenteCnpj())
                    || snapshot.ambiente() != documento.ambiente())
                throw CadastroSupport.invalido("Snapshot fiscal divergente da nova entrada.");
            var autorizacao = xml.conferir(documento.xml(), snapshot);
            var parcelas = new ArrayList<Parcela>();
            for (var item : snapshot.itens()) {
                var reserva =
                        linhas.stream()
                                .filter(r -> Objects.equals(r.getId(), item.reservaId()))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                CadastroSupport.invalido(
                                                        "Reserva de origem ausente."));
                var unidade = reserva.getUnidade();
                var origem =
                        conteudos.buscarOrigens(unidade.getId()).stream()
                                .filter(
                                        c ->
                                                Objects.equals(
                                                        c.getEntrada().getId(),
                                                        item.entradaOrigemId()))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                CadastroSupport.invalido(
                                                        "Parcela de origem ausente."));
                String chave = unidade.getId() + ":" + item.entradaOrigemId();
                BigDecimal total = comprometidas.merge(chave, item.quantidade(), BigDecimal::add);
                if (item.quantidade().signum() <= 0
                        || total.compareTo(origem.getQuantidade()) > 0
                        || !Objects.equals(unidade.getProduto().getId(), item.produtoId())
                        || !Objects.equals(
                                origem.getEntrada().getItemChegada().getItemNota().getId(),
                                item.itemNotaOrigemId()))
                    throw CadastroSupport.invalido(
                            "A composição presente não corresponde às parcelas da nova entrada fiscal.");
                parcelas.add(new Parcela(item, reserva, origem));
            }
            if (parcelas.isEmpty()) throw CadastroSupport.invalido("Entrada fiscal sem parcelas.");
            planos.add(new Plano(documento, autorizacao, List.copyOf(parcelas)));
        }
        String motivo =
                "Regularização fiscal sem trânsito externo; mercadoria já presente no armazém.";
        var pedido =
                pedidos.save(
                        new PedidoEntrada(
                                saida.getCliente(),
                                saida.getArmazem(),
                                CodigoInterno.resolver(null, "ENT", pedidos::ultimoCodigoInterno),
                                agora));
        var chegada =
                chegadas.save(
                        new ChegadaRecebimento(
                                pedido,
                                operacao.toString(),
                                NfeXmlService.hash(
                                        mapper.writeValueAsString(
                                                documentos.stream()
                                                        .map(SolicitacaoNfe::id)
                                                        .toList())),
                                agora,
                                agora,
                                acesso.usuario(),
                                motivo));
        int sequencia = 0;
        var revisadas = new HashSet<Long>();
        for (var plano : planos) {
            var fiscal = plano.autorizacao().nota();
            var nota =
                    notas.save(
                            new NotaEntrada(
                                    pedido,
                                    fiscal.emitenteCnpj(),
                                    Integer.parseInt(fiscal.serie()),
                                    Long.parseLong(fiscal.numero()),
                                    fiscal.emissao(),
                                    fiscal.chaveAcesso()));
            int numeroItem = 0;
            for (var parcela : plano.parcelas()) {
                var item = parcela.item();
                var unidade = parcela.reserva().getUnidade();
                var anterior = parcela.origem().getEntrada();
                var itemOrigem = anterior.getItemChegada();
                var novaLinha =
                        itens.save(
                                new ItemNotaEntrada(
                                        nota,
                                        ++numeroItem,
                                        unidade.getProduto(),
                                        item.quantidade(),
                                        item.valorTotal()));
                boolean boa = unidade.getCondicao() == CondicaoMercadoria.BOA;
                var recebido =
                        itensChegada.save(
                                new ItemChegada(
                                        chegada,
                                        ++sequencia,
                                        novaLinha,
                                        itemOrigem.getLote(),
                                        itemOrigem.getValidade(),
                                        boa ? item.quantidade() : BigDecimal.ZERO,
                                        boa ? BigDecimal.ZERO : item.quantidade()));
                var entrada =
                        entradas.save(
                                new EntradaConferida(
                                        recebido,
                                        anterior.getDataFifo(),
                                        agora,
                                        boa ? item.quantidade() : BigDecimal.ZERO,
                                        boa ? BigDecimal.ZERO : item.quantidade()));
                entrada.marcarUnitizada(agora);
                parcela.origem()
                        .alterarQuantidade(
                                parcela.origem().getQuantidade().subtract(item.quantidade()));
                conteudos.save(new ConteudoUnidade(unidade, entrada, item.quantidade()));
                if (revisadas.add(unidade.getId()))
                    unidade.alterarQuantidade(unidade.getQuantidade(), agora);
            }
        }
        pedido.concluir(motivo, agora);
        pedidos.flush();
        auditoria.registrar(
                "PEDIDO_ENTRADA",
                pedido.getId(),
                "ENTRADA_EFETIVADA",
                motivo,
                null,
                Map.of(
                        "tipoEntrada",
                        "REGULARIZACAO_SEM_TRANSITO",
                        "pedidoSaidaId",
                        saida.getId(),
                        "solicitacoes",
                        documentos.stream().map(SolicitacaoNfe::id).toList(),
                        "unidades",
                        revisadas,
                        "quantidadeFisicaAlterada",
                        false));
        return pedido.getId();
    }
}
