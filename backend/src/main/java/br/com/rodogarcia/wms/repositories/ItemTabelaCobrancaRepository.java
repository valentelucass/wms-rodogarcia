package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemTabelaCobrancaRepository extends JpaRepository<ItemTabelaCobranca, Long> {
    java.util.List<ItemTabelaCobranca> findByTabelaIdOrderByIdAsc(Long tabelaId);
}
