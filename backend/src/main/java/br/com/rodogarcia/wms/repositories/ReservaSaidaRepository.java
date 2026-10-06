package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ReservaSaida;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservaSaidaRepository extends JpaRepository<ReservaSaida, Long> {
    java.util.List<ReservaSaida> findByUnidadeIdAndSituacao(
            Long unidadeId, br.com.rodogarcia.wms.models.SituacaoReservaSaida situacao);

    @Query(
            "select r from ReservaSaida r join fetch r.unidade u join fetch r.item i where r.pedido.id=:pedidoId order by r.id")
    List<ReservaSaida> buscarDoPedido(Long pedidoId);

    @Query(
            "select sum(r.quantidade) from ReservaSaida r where r.situacao=br.com.rodogarcia.wms.models.SituacaoReservaSaida.ATIVA and r.pedido.cliente.id=:clienteId and r.pedido.armazem.id=:armazemId and r.item.produto.id=:produtoId")
    BigDecimal somarAtiva(Long clienteId, Long armazemId, Long produtoId);
}
