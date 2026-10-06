package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ResolucaoFinanceiraFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolucaoFinanceiraFechamentoRepository
        extends JpaRepository<ResolucaoFinanceiraFechamento, Long> {
    java.util.Optional<ResolucaoFinanceiraFechamento> findByVersaoId(Long id);
}
