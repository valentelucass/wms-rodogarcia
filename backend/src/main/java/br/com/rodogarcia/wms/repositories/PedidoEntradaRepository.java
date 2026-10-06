package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.PedidoEntrada;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PedidoEntradaRepository extends JpaRepository<PedidoEntrada, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoEntrada p where p.id = :id")
    Optional<PedidoEntrada> buscarParaAtualizar(Long id);

    Page<PedidoEntrada> findByClienteIdAndArmazemId(
            Long clienteId, Long armazemId, Pageable pageable);
}
