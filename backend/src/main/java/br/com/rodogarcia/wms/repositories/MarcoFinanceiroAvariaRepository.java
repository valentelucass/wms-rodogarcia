package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.MarcoFinanceiroAvaria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarcoFinanceiroAvariaRepository
        extends JpaRepository<MarcoFinanceiroAvaria, Long> {
    java.util.List<MarcoFinanceiroAvaria> findByAvariaIdOrderByIdAsc(Long avariaId);

    boolean existsByAvariaIdAndFatoPermanenciaId(Long avariaId, Long fatoPermanenciaId);

    @org.springframework.data.jpa.repository.Query(
            "select m from MarcoFinanceiroAvaria m join fetch m.avaria a join fetch m.fatoPermanencia where a.unidade.id in :ids order by a.unidade.id,a.id,m.id")
    java.util.List<MarcoFinanceiroAvaria> buscarParaIndicador(java.util.List<Long> ids);
}
