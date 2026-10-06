package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ConfirmacaoExternaFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmacaoExternaFechamentoRepository
        extends JpaRepository<ConfirmacaoExternaFechamento, Long> {
    java.util.List<ConfirmacaoExternaFechamento> findByVersaoIdOrderByIdAsc(Long id);
}
