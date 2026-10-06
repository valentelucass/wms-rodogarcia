package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.EntradaConferida;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntradaConferidaRepository extends JpaRepository<EntradaConferida, Long> {
    @Query(
            "select e from EntradaConferida e where e.itemChegada.chegada.pedido.cliente.id=:clienteId and e.itemChegada.chegada.pedido.armazem.id=:armazemId order by e.id")
    java.util.List<EntradaConferida> historicoCobranca(Long clienteId, Long armazemId);

    @EntityGraph(
            attributePaths = {
                "itemChegada",
                "itemChegada.chegada",
                "itemChegada.itemNota",
                "itemChegada.itemNota.nota",
                "itemChegada.itemNota.produto"
            })
    Page<EntradaConferida> findByItemChegadaChegadaPedidoId(Long pedidoId, Pageable pageable);

    Optional<EntradaConferida> findByIdAndItemChegadaChegadaPedidoId(Long id, Long pedidoId);

    long countByItemChegadaChegadaPedidoId(Long pedidoId);

    long countByItemChegadaChegadaPedidoIdAndUnitizadaEmIsNotNull(Long pedidoId);

    @Query(
            "select sum(e.quantidadeTriagem + e.quantidadeQuarentena) from EntradaConferida e where e.unitizadaEm is null and e.itemChegada.chegada.pedido.cliente.id = :clienteId and e.itemChegada.chegada.pedido.armazem.id = :armazemId and e.itemChegada.itemNota.produto.id = :produtoId")
    BigDecimal quantidadePendenteUnitizacao(Long clienteId, Long armazemId, Long produtoId);
}
