package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.EntregaEsl;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaEslRepository extends JpaRepository<EntregaEsl, Long> {
    java.util.List<EntregaEsl> findByVersaoIdOrderBySequenciaAsc(Long id);
}
