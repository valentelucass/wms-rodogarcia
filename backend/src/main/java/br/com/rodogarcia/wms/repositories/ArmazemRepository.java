package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.Armazem;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArmazemRepository extends JpaRepository<Armazem, Long> {
    Page<Armazem> findByIdIn(Collection<Long> ids, Pageable pageable);

    java.util.List<Armazem> findByDocumentoFiscal(String documentoFiscal);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Armazem c where c.id = :id")
    Optional<Armazem> buscarParaAtualizar(@Param("id") Long id);
}
