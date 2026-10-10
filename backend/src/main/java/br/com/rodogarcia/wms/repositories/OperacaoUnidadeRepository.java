package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.OperacaoUnidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoUnidadeRepository extends JpaRepository<OperacaoUnidade, Long> {
    Optional<OperacaoUnidade> findByPedidoIdAndOperacaoId(Long pedidoId, String operacaoId);

    List<OperacaoUnidade> findByPedidoIdAndTipoInOrderByRegistradaEmAscIdAsc(
            Long pedidoId, List<String> tipos);

    interface TransformacaoIndicador {
        Long getPedidoId();

        OperacaoUnidade getOperacao();
    }

    @org.springframework.data.jpa.repository.Query(
            "select o.pedido.id as pedidoId,o as operacao from OperacaoUnidade o where o.pedido.id in :ids and o.tipo in ('UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS') order by o.pedido.id,o.registradaEm,o.id")
    List<TransformacaoIndicador> buscarParaIndicador(List<Long> ids);
}
