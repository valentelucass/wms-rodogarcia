package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.PedidoSaida;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PedidoSaidaRepository extends JpaRepository<PedidoSaida, Long> {
    interface Escopo {
        Long getClienteId();

        Long getArmazemId();
    }

    @Query(
            "select p.cliente.id as clienteId,p.armazem.id as armazemId from PedidoSaida p where p.id=:id")
    Optional<Escopo> buscarEscopo(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PedidoSaida p where p.id=:id")
    Optional<PedidoSaida> buscarParaAtualizar(Long id);

    Optional<PedidoSaida> findByClienteIdAndArmazemIdAndReferencia(
            Long clienteId, Long armazemId, String referencia);

    Page<PedidoSaida> findByClienteIdAndArmazemId(Long clienteId, Long armazemId, Pageable pagina);
}
