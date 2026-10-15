package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemTabelaCobrancaRepository extends JpaRepository<ItemTabelaCobranca, Long> {
    /** Itens retirados continuam acessíveis pelas referências das memórias financeiras. */
    @org.springframework.data.jpa.repository.Query(
            "select i from ItemTabelaCobranca i where i.tabela.id=:tabelaId and i.ativo=true order by i.id")
    java.util.List<ItemTabelaCobranca> findByTabelaIdOrderByIdAsc(Long tabelaId);
}
