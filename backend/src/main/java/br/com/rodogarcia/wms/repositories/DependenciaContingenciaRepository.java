package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.DependenciaContingencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependenciaContingenciaRepository
        extends JpaRepository<DependenciaContingencia, Long> {
    boolean existsByLinhaIdAndDependeId(Long linhaId, Long dependeId);
}
