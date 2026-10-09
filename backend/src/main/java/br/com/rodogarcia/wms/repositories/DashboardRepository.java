package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Agregados de leitura: nenhum carregamento de todas as unidades ou escrita. */
public interface DashboardRepository extends Repository<UnidadeLogistica, Long> {
    interface AreaCliente {
        TipoEndereco getTipo();

        long getPosicoes();
    }

    interface AreaArmazem {
        TipoEndereco getTipo();

        long getCapacidade();

        long getOcupadas();
    }

    interface Fila {
        SituacaoPedidoSaida getSituacao();

        long getPedidos();
    }

    interface Quantidade {
        Long getProdutoId();

        BigDecimal getQuantidade();
    }

    @Query(
            "select o.endereco.tipo as tipo,count(o.id) as posicoes from OcupacaoEndereco o where o.unidade.ativa=true and o.unidade.pedido.cliente.id=:clienteId and o.endereco.armazem.id=:armazemId group by o.endereco.tipo")
    List<AreaCliente> ocupacaoCliente(Long clienteId, Long armazemId);

    @Query(
            "select e.tipo as tipo,count(e.id) as capacidade,count(o.unidade.id) as ocupadas from Endereco e left join OcupacaoEndereco o on o.endereco.id=e.id where e.armazem.id=:armazemId and e.situacao=br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO group by e.tipo")
    List<AreaArmazem> capacidadeArmazem(Long armazemId);

    @Query(
            "select count(u.id) from UnidadeLogistica u where u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and ("
                    + UnidadeLogisticaRepository.DISPONIVEL
                    + ")")
    long unidadesDisponiveis(Long clienteId, Long armazemId);

    @Query(
            "select count(u.id) from UnidadeLogistica u where u.ativa=true and u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and u.validade<=:limite")
    long unidadesComAviso(Long clienteId, Long armazemId, LocalDate limite);

    @Query(
            "select p.situacao as situacao,count(p.id) as pedidos from PedidoSaida p where p.cliente.id=:clienteId and p.armazem.id=:armazemId group by p.situacao")
    List<Fila> fila(Long clienteId, Long armazemId);

    @Query(
            "select u.produto.id as produtoId,sum(u.quantidade) as quantidade from UnidadeLogistica u where u.ativa=true and u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and u.produto.id in :produtos group by u.produto.id")
    List<Quantidade> fisico(Long clienteId, Long armazemId, List<Long> produtos);

    @Query(
            "select u.produto.id as produtoId,sum(u.quantidade) as quantidade from UnidadeLogistica u where u.pedido.cliente.id=:clienteId and u.pedido.armazem.id=:armazemId and u.produto.id in :produtos and ("
                    + UnidadeLogisticaRepository.DISPONIVEL
                    + ") group by u.produto.id")
    List<Quantidade> disponivel(Long clienteId, Long armazemId, List<Long> produtos);

    @Query(
            "select r.item.produto.id as produtoId,sum(r.quantidade) as quantidade from ReservaSaida r where r.situacao=br.com.rodogarcia.wms.models.SituacaoReservaSaida.ATIVA and r.pedido.cliente.id=:clienteId and r.pedido.armazem.id=:armazemId and r.item.produto.id in :produtos group by r.item.produto.id")
    List<Quantidade> reservado(Long clienteId, Long armazemId, List<Long> produtos);

    @Query(
            "select e.itemChegada.itemNota.produto.id as produtoId,sum(e.quantidadeTriagem+e.quantidadeQuarentena) as quantidade from EntradaConferida e where e.unitizadaEm is null and e.itemChegada.chegada.pedido.cliente.id=:clienteId and e.itemChegada.chegada.pedido.armazem.id=:armazemId and e.itemChegada.itemNota.produto.id in :produtos group by e.itemChegada.itemNota.produto.id")
    List<Quantidade> pendente(Long clienteId, Long armazemId, List<Long> produtos);
}
