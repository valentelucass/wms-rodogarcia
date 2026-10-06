package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ConfiguracaoAvisoValidade;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracaoAvisoValidadeRepository
        extends JpaRepository<ConfiguracaoAvisoValidade, Long> {
    Optional<ConfiguracaoAvisoValidade> findByClienteIdAndArmazemId(Long clienteId, Long armazemId);
}
