package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.TabelaCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TabelaCobrancaRepository extends JpaRepository<TabelaCobranca, Long> {
    boolean existsByArmazemIdAndCodigo(Long armazemId, String codigo);

    java.util.List<TabelaCobranca> findByArmazemIdOrderByIdAsc(Long armazemId);

    @org.springframework.data.jpa.repository.Query(
            "select t.armazem.id from TabelaCobranca t where t.id=:id")
    java.util.Optional<Long> buscarArmazemId(Long id);
}
