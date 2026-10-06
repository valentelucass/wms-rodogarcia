package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.DevolucaoSaida;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.ChegadaRecebimentoRepository;
import br.com.rodogarcia.wms.repositories.DevolucaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.ItemChegadaRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class RegistroDevolucaoService {
    private final PedidoEntradaRepository pedidos;
    private final NotaEntradaRepository notas;
    private final ItemNotaEntradaRepository itens;
    private final ChegadaRecebimentoRepository chegadas;
    private final ItemChegadaRepository itensChegada;
    private final EntradaConferidaRepository entradas;
    private final BaixaSaidaRepository baixas;
    private final DevolucaoSaidaRepository devolucoes;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;

    public RegistroDevolucaoService(
            PedidoEntradaRepository pedidos,
            NotaEntradaRepository notas,
            ItemNotaEntradaRepository itens,
            ChegadaRecebimentoRepository chegadas,
            ItemChegadaRepository itensChegada,
            EntradaConferidaRepository entradas,
            BaixaSaidaRepository baixas,
            DevolucaoSaidaRepository devolucoes,
            AcessoService acesso,
            AuditoriaService auditoria) {
        this.pedidos = pedidos;
        this.notas = notas;
        this.itens = itens;
        this.chegadas = chegadas;
        this.itensChegada = itensChegada;
        this.entradas = entradas;
        this.baixas = baixas;
        this.devolucoes = devolucoes;
        this.acesso = acesso;
        this.auditoria = auditoria;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Long registrar(PedidoSaida saida, ExpedicaoDto.Devolver d, String hash, Instant agora) {
        acesso.exigirSupervisor();
        acesso.cliente(saida.getCliente().getId());
        acesso.armazem(saida.getArmazem().getId());
        if (d.resolverPendentes()) acesso.exigirGestor();
        if (d.chegadaReal().isAfter(agora)
                || d.chegadaReal()
                        .isBefore(
                                baixas.buscarDoPedido(saida.getId())
                                        .getFirst()
                                        .getRetirada()
                                        .getRetiradaEm()))
            throw CadastroSupport.invalido("Chegada deve ocorrer após retirada e não no futuro.");
        if (d.nota().emissao().isAfter(LocalDate.ofInstant(agora, java.time.ZoneOffset.UTC)))
            throw CadastroSupport.invalido("Emissão futura.");
        var originais = baixas.buscarDoPedido(saida.getId());
        var vistos = new HashSet<Long>();
        for (var i : d.itens()) {
            if (!vistos.add(i.baixaId()))
                throw CadastroSupport.invalido("Origem de devolução repetida.");
            var b =
                    originais.stream()
                            .filter(a -> a.getId().equals(i.baixaId()))
                            .findFirst()
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            var produto = b.getReserva().getUnidade().getProduto();
            CadastroSupport.quantidade(produto, i.quantidade());
            if (i.quantidadeAvariada().signum() > 0)
                CadastroSupport.quantidade(produto, i.quantidadeAvariada());
            if (i.quantidadeAvariada().compareTo(i.quantidade()) > 0)
                throw CadastroSupport.invalido("Avaria excede a quantidade devolvida.");
            BigDecimal anterior = devolucoes.somarDaBaixa(b.getId());
            if (anterior == null) anterior = BigDecimal.ZERO;
            if (anterior.add(i.quantidade()).compareTo(b.getQuantidade()) > 0)
                throw RegraNegocioException.conflito(
                        "DEVOLUCAO_EXCEDIDA", "Devolução excede quantidade retirada da origem.");
        }
        var p =
                pedidos.save(
                        new PedidoEntrada(
                                saida.getCliente(),
                                saida.getArmazem(),
                                CadastroSupport.codigo(d.referencia()),
                                agora));
        var n = d.nota();
        var nota =
                notas.save(
                        new NotaEntrada(
                                p,
                                n.emitenteCnpj(),
                                Integer.parseInt(n.serie()),
                                Long.parseLong(n.numero()),
                                n.emissao(),
                                n.chaveAcesso()));
        var chegada =
                chegadas.save(
                        new ChegadaRecebimento(
                                p,
                                d.operacaoId().toString(),
                                hash,
                                d.chegadaReal(),
                                agora,
                                acesso.usuario(),
                                CadastroSupport.motivo(d.motivo())));
        int seq = 0;
        for (var i : d.itens()) {
            var b =
                    originais.stream()
                            .filter(a -> a.getId().equals(i.baixaId()))
                            .findFirst()
                            .orElseThrow();
            var origem = b.getEntradaOrigem().getItemChegada();
            var item =
                    itens.save(
                            new ItemNotaEntrada(
                                    nota,
                                    ++seq,
                                    b.getReserva().getUnidade().getProduto(),
                                    i.quantidade(),
                                    null));
            var recebido =
                    itensChegada.save(
                            new ItemChegada(
                                    chegada,
                                    seq,
                                    item,
                                    origem.getLote(),
                                    origem.getValidade(),
                                    i.quantidade().subtract(i.quantidadeAvariada()),
                                    i.quantidadeAvariada()));
            var entrada =
                    entradas.save(
                            new EntradaConferida(
                                    recebido,
                                    b.getDataFifo(),
                                    agora,
                                    i.quantidade().subtract(i.quantidadeAvariada()),
                                    i.quantidadeAvariada()));
            devolucoes.save(new DevolucaoSaida(b, p, entrada, i.quantidade(), agora));
        }
        p.concluir(CadastroSupport.motivo(d.motivo()), agora);
        pedidos.flush();
        auditoria.registrar(
                "PEDIDO_ENTRADA",
                p.getId(),
                "DEVOLUCAO_ENTRADA",
                CadastroSupport.motivo(d.motivo()),
                null,
                java.util.Map.of(
                        "pedidoSaidaId",
                        saida.getId(),
                        "pedidoEntradaId",
                        p.getId(),
                        "operacaoId",
                        d.operacaoId(),
                        "resolverPendentes",
                        d.resolverPendentes()));
        return p.getId();
    }
}
