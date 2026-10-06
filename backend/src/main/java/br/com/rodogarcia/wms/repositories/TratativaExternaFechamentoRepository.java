package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.TratativaExternaFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TratativaExternaFechamentoRepository
        extends JpaRepository<TratativaExternaFechamento, Long> {
    java.util.List<TratativaExternaFechamento> findByFechamentoIdOrderByIdAsc(Long id);
}
