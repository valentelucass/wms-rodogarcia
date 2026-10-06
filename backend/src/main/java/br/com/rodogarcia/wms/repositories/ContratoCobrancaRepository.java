package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ContratoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoCobrancaRepository extends JpaRepository<ContratoCobranca, Long> {
    java.util.List<ContratoCobranca> findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
            Long clienteId, Long armazemId);

    @org.springframework.data.jpa.repository.Query(
            "select c.cliente.id as clienteId,c.armazem.id as armazemId from ContratoCobranca c where c.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);
}
