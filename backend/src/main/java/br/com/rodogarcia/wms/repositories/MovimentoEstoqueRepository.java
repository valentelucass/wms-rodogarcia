package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.MovimentoEstoque;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentoEstoqueRepository extends JpaRepository<MovimentoEstoque, Long> {
    Optional<MovimentoEstoque> findByPedidoIdAndOperacaoId(Long pedidoId, String operacaoId);

    Page<MovimentoEstoque> findByUnidadeId(Long unidadeId, Pageable pageable);

    List<MovimentoEstoque> findByUnidadeIdOrderByInstanteAscIdAsc(Long unidadeId);

    interface MovimentoIndicador {
        Long getUnidadeId();

        MovimentoEstoque getMovimento();
    }

    @org.springframework.data.jpa.repository.Query(
            "select m.unidade.id as unidadeId,m as movimento from MovimentoEstoque m where m.unidade.id in :ids order by m.unidade.id,m.instante,m.id")
    List<MovimentoIndicador> buscarParaIndicador(List<Long> ids);
}
