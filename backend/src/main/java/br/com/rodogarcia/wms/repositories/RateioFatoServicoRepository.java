package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.RateioFatoServico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateioFatoServicoRepository extends JpaRepository<RateioFatoServico, Long> {
    java.util.List<RateioFatoServico> findByFatoIdOrderByNotaIdAsc(Long fatoId);
}
