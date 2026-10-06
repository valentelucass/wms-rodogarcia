package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.AjusteFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AjusteFechamentoRepository extends JpaRepository<AjusteFechamento, Long> {
    java.util.List<AjusteFechamento> findByOrigemVersaoIdOrderByIdAsc(Long id);

    java.util.List<AjusteFechamento> findByOrigemVersaoFechamentoIdOrderByIdAsc(Long id);

    java.util.List<AjusteFechamento> findByDestinoFechamentoIdOrderByIdAsc(Long id);
}
