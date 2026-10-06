package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ItemPedidoSaida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemPedidoSaidaRepository extends JpaRepository<ItemPedidoSaida, Long> {
    @Query(
            "select i from ItemPedidoSaida i join fetch i.produto where i.pedido.id=:pedidoId order by i.produto.id")
    List<ItemPedidoSaida> buscarDoPedido(Long pedidoId);
}
