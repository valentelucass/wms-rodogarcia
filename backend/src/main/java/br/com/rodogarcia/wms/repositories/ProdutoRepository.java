package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.Produto;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    @Query("select p.id from Produto p where p.cliente.id = :clienteId and p.sku = :sku")
    Optional<Long> buscarIdPorSku(Long clienteId, String sku);

    Page<Produto> findByClienteId(Long clienteId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            "select p from Produto p where p.cliente.id=:clienteId and (:produtoId is null or p.id=:produtoId)")
    Page<Produto> consultarIndicadores(Long clienteId, Long produtoId, Pageable pagina);

    @Query("select p.cliente.id from Produto p where p.id = :id")
    Optional<Long> buscarClienteId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Produto c where c.id = :id")
    Optional<Produto> buscarParaAtualizar(@Param("id") Long id);
}
