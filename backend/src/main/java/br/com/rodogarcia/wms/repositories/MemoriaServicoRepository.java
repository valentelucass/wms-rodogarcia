package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.MemoriaServico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoriaServicoRepository extends JpaRepository<MemoriaServico, Long> {
    java.util.List<MemoriaServico> findByCalculoIdOrderByIdAsc(Long calculoId);
}
