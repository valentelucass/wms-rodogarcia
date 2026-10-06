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
}
