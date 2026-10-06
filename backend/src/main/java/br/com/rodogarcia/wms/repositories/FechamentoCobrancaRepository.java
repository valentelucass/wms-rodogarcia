package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.FechamentoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FechamentoCobrancaRepository extends JpaRepository<FechamentoCobranca, Long> {
    java.util.List<FechamentoCobranca> findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc(
            Long clienteId, Long armazemId);

    org.springframework.data.domain.Page<FechamentoCobranca> findByClienteIdAndArmazemId(
            Long clienteId, Long armazemId, org.springframework.data.domain.Pageable pagina);

    @Query(
            "select f.cliente.id as clienteId,f.armazem.id as armazemId from FechamentoCobranca f where f.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FechamentoCobranca f where f.id=:id")
    java.util.Optional<FechamentoCobranca> buscarParaAtualizar(Long id);
}
