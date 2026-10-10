package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.OperacaoAdministrativa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoAdministrativaRepository
        extends JpaRepository<OperacaoAdministrativa, Long> {
    java.util.Optional<OperacaoAdministrativa> findByOperacaoId(String operacaoId);

    java.util.Optional<OperacaoAdministrativa> findFirstByTipoAndRecursoIdOrderByIdAsc(
            String tipo, Long recursoId);

    java.util.List<OperacaoAdministrativa> findByTipoAndRecursoIdIn(
            String tipo, java.util.Collection<Long> recursoIds);
}
