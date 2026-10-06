package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.BaixaSaida;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BaixaSaidaRepository extends JpaRepository<BaixaSaida, Long> {
    interface SugestaoQuantidade {
        Long getRetiradaId();

        Long getPedidoSaidaId();

        Long getProdutoId();

        java.time.Instant getExecutadoEm();

        BigDecimal getQuantidade();
    }

    @Query(
            value =
                    "select r.id as retiradaId,r.pedido.id as pedidoSaidaId,u.produto.id as produtoId,r.retiradaEm as executadoEm,sum(b.quantidade) as quantidade from BaixaSaida b join b.retirada r join b.reserva.unidade u where r.pedido.cliente.id=:clienteId and r.pedido.armazem.id=:armazemId and not exists(select f.id from FatoServico f where f.servico.id=:servicoId and f.pedidoSaida.id=r.pedido.id and f.produto.id=u.produto.id) group by r.id,r.pedido.id,u.produto.id,r.retiradaEm",
            countQuery =
                    "select count(distinct concat(cast(r.id as string),':',cast(u.produto.id as string))) from BaixaSaida b join b.retirada r join b.reserva.unidade u where r.pedido.cliente.id=:clienteId and r.pedido.armazem.id=:armazemId and not exists(select f.id from FatoServico f where f.servico.id=:servicoId and f.pedidoSaida.id=r.pedido.id and f.produto.id=u.produto.id)")
    org.springframework.data.domain.Page<SugestaoQuantidade> candidatasQuantidade(
            Long clienteId,
            Long armazemId,
            Long servicoId,
            org.springframework.data.domain.Pageable pagina);

    @Query("select b from BaixaSaida b where b.retirada.pedido.id=:pedidoId order by b.id")
    List<BaixaSaida> buscarDoPedido(Long pedidoId);

    @Query(
            "select coalesce(sum(b.quantidade),0) from BaixaSaida b where b.entradaOrigem.id=:entradaId and b.reserva.unidade.condicao=:condicao")
    BigDecimal somarOrigemCondicao(
            Long entradaId, br.com.rodogarcia.wms.models.CondicaoMercadoria condicao);

    @org.springframework.data.jpa.repository.Query(
            "select b from BaixaSaida b where b.reserva.unidade.id=:unidadeId order by b.id")
    java.util.List<BaixaSaida> buscarDaUnidade(Long unidadeId);
}
