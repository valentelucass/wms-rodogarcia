package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.RenovacaoAcesso;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RenovacaoAcessoRepository extends JpaRepository<RenovacaoAcesso, String> {
    @Query("select r.sessaoId from RenovacaoAcesso r where r.hash = :hash")
    Optional<String> sessaoDoToken(@Param("hash") String hash);
}
