package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.UsuarioAcesso;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioAcessoRepository extends JpaRepository<UsuarioAcesso, String> {
    Optional<UsuarioAcesso> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioAcesso u where u.email = :email")
    Optional<UsuarioAcesso> porEmailComLock(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioAcesso u where u.id = :id")
    Optional<UsuarioAcesso> porIdComLock(@Param("id") String id);
}
