package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.AjusteVersaoFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AjusteVersaoFechamentoRepository
        extends JpaRepository<AjusteVersaoFechamento, Long> {
    java.util.List<AjusteVersaoFechamento> findByVersaoIdOrderByIdAsc(Long id);
}
