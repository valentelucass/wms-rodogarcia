package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VinculoTabelaClienteRepository extends JpaRepository<VinculoTabelaCliente, Long> {
    java.util.List<VinculoTabelaCliente> findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
            Long clienteId, Long armazemId);

    @org.springframework.data.jpa.repository.Query(
            "select c.cliente.id as clienteId,c.armazem.id as armazemId from VinculoTabelaCliente c where c.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);
}
