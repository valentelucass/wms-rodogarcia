package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.FatoFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FatoFechamentoRepository extends JpaRepository<FatoFechamento, Long> {
    java.util.Optional<FatoFechamento> findByFatoId(Long id);
}
