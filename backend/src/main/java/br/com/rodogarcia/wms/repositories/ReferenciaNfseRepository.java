package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ReferenciaNfse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenciaNfseRepository extends JpaRepository<ReferenciaNfse, Long> {
    boolean existsByEmissorDocumentoAndReferenciaExterna(String emissor, String referencia);

    java.util.List<ReferenciaNfse> findByVersaoIdOrderByIdAsc(Long id);
}
