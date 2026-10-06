package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.OperacaoUnidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoUnidadeRepository extends JpaRepository<OperacaoUnidade, Long> {
    Optional<OperacaoUnidade> findByPedidoIdAndOperacaoId(Long pedidoId, String operacaoId);

    List<OperacaoUnidade> findByPedidoIdAndTipoInOrderByRegistradaEmAscIdAsc(
            Long pedidoId, List<String> tipos);
}
