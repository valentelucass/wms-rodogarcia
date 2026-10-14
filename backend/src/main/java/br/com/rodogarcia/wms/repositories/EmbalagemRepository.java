package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.Embalagem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmbalagemRepository extends JpaRepository<Embalagem, Long> {
    @Query("select count(u) from UnidadeLogistica u where u.embalagem.id=:id")
    long contarUnidades(Long id);

    Page<Embalagem> findByProdutoId(Long produtoId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Embalagem c where c.id = :id")
    Optional<Embalagem> buscarParaAtualizar(@Param("id") Long id);
}
