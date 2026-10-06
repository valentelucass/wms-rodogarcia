package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ServicoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoCobrancaRepository extends JpaRepository<ServicoCobranca, Long> {
    boolean existsByCodigo(String codigo);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from ServicoCobranca s where s.id=:id")
    java.util.Optional<ServicoCobranca> buscarParaAtualizar(Long id);
}
