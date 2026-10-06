package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ServicoMinimoContrato;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoMinimoContratoRepository
        extends JpaRepository<ServicoMinimoContrato, Long> {
    java.util.List<ServicoMinimoContrato> findByContratoId(Long contratoId);
}
