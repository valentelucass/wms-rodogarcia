package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.RevisaoContagem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisaoContagemRepository extends JpaRepository<RevisaoContagem, Long> {
    java.util.List<RevisaoContagem> findByContagemUnidadeIdAndSituacao(
            Long unidadeId, br.com.rodogarcia.wms.models.SituacaoRevisaoContagem situacao);

    Optional<RevisaoContagem> findByContagemIdAndNumero(Long contagemId, int numero);

    Page<RevisaoContagem> findByContagemId(Long contagemId, Pageable pagina);

    @org.springframework.data.jpa.repository.Query(
            "select r from RevisaoContagem r where r.contagem.id=:contagemId and (:situacao is null or r.situacao=:situacao)")
    Page<RevisaoContagem> consultar(
            Long contagemId,
            br.com.rodogarcia.wms.models.SituacaoRevisaoContagem situacao,
            Pageable pagina);

    @org.springframework.data.jpa.repository.Query(
            "select r from RevisaoContagem r join fetch r.contagem c where c.unidade.id in :ids and r.situacao=br.com.rodogarcia.wms.models.SituacaoRevisaoContagem.APLICADA")
    java.util.List<RevisaoContagem> buscarParaIndicador(java.util.List<Long> ids);
}
