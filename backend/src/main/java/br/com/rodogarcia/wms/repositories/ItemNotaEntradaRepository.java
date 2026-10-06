package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemNotaEntradaRepository extends JpaRepository<ItemNotaEntrada, Long> {
    @Query(
            "select distinct i.produto.id from ItemNotaEntrada i where i.nota.pedido.id = :pedidoId order by i.produto.id")
    List<Long> buscarProdutosIds(Long pedidoId);

    @Query(
            "select i from ItemNotaEntrada i join fetch i.nota n join fetch i.produto where n.pedido.id = :pedidoId order by i.id")
    List<ItemNotaEntrada> buscarDoPedido(Long pedidoId);

    List<ItemNotaEntrada> findByNotaIdOrderByNumeroItem(Long notaId);

    long countByNotaPedidoId(Long pedidoId);
}
