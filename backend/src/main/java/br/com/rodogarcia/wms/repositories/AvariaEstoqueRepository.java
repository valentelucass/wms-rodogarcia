package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.AvariaEstoque;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvariaEstoqueRepository extends JpaRepository<AvariaEstoque, Long> {
    List<AvariaEstoque> findByUnidadeIdOrderById(Long unidadeId);

    @Query(
            "select sum(a.quantidade) from AvariaEstoque a where a.unidade.id=:unidadeId and a.tratativa='EM_TRATAMENTO'")
    BigDecimal somarAbertas(Long unidadeId);

    @Query(
            "select a.unidade.pedido.cliente.id as clienteId,a.unidade.pedido.armazem.id as armazemId from AvariaEstoque a where a.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);

    @Query("select a from AvariaEstoque a where a.unidade.id in :ids order by a.unidade.id,a.id")
    List<AvariaEstoque> buscarParaIndicador(List<Long> ids);
}
