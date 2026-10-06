package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.VersaoFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersaoFechamentoRepository extends JpaRepository<VersaoFechamento, Long> {
    java.util.List<VersaoFechamento> findByFechamentoIdOrderByNumeroAsc(Long id);

    java.util.Optional<VersaoFechamento> findByFechamentoIdAndNumero(Long id, int numero);
}
