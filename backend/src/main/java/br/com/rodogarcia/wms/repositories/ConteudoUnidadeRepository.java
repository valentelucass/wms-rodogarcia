package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ConteudoUnidade;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConteudoUnidadeRepository extends JpaRepository<ConteudoUnidade, Long> {
    @Query(
            "select c from ConteudoUnidade c join fetch c.entrada e join fetch e.itemChegada i join fetch i.itemNota where c.unidade.id = :unidadeId order by e.id")
    List<ConteudoUnidade> buscarOrigens(Long unidadeId);

    @Query(
            "select coalesce(sum(c.quantidade),0) from ConteudoUnidade c where c.entrada.id=:entradaId and c.unidade.condicao=:condicao")
    java.math.BigDecimal somarOrigemCondicao(
            Long entradaId, br.com.rodogarcia.wms.models.CondicaoMercadoria condicao);

    @Query(
            "select c from ConteudoUnidade c join fetch c.entrada e join fetch e.itemChegada i join fetch i.itemNota where c.unidade.id in :ids order by c.unidade.id,e.id")
    List<ConteudoUnidade> buscarParaIndicador(List<Long> ids);
}
