package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.Cliente;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Page<Cliente> findByIdIn(Collection<Long> ids, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cliente c where c.id = :id")
    Optional<Cliente> buscarParaAtualizar(@Param("id") Long id);
}
