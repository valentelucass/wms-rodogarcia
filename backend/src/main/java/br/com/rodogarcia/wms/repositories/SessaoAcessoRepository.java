package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.SessaoAcesso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoAcessoRepository extends JpaRepository<SessaoAcesso, String> {
    @Query("select s.usuarioId from SessaoAcesso s where s.id = :id")
    Optional<String> usuarioDaSessao(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoAcesso s where s.id = :id")
    Optional<SessaoAcesso> comLock(@Param("id") String id);
}
