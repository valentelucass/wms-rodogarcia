package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.MarcoFinanceiroAvaria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcoFinanceiroAvariaRepository
        extends JpaRepository<MarcoFinanceiroAvaria, Long> {
    java.util.List<MarcoFinanceiroAvaria> findByAvariaIdOrderByIdAsc(Long avariaId);

    boolean existsByAvariaIdAndFatoPermanenciaId(Long avariaId, Long fatoPermanenciaId);
}
