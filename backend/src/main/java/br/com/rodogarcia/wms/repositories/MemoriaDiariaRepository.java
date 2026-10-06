package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.MemoriaDiaria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoriaDiariaRepository extends JpaRepository<MemoriaDiaria, Long> {
    java.util.List<MemoriaDiaria> findByCalculoIdOrderByDataAsc(Long calculoId);

    boolean existsByTabelaIdAndDataGreaterThanEqual(Long tabelaId, java.time.LocalDate data);
}
