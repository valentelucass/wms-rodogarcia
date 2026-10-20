package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ContratoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratoCobrancaRepository extends JpaRepository<ContratoCobranca, Long> {
    @org.springframework.data.jpa.repository.Query(
            "select c from ContratoCobranca c where c.cliente.id in :clientes and c.armazem.id in :armazens order by c.cliente.id,c.armazem.id,c.vigenciaInicio")
    java.util.List<ContratoCobranca> paraPrevisao(
            java.util.List<Long> clientes, java.util.List<Long> armazens);

    java.util.List<ContratoCobranca> findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
            Long clienteId, Long armazemId);

    @org.springframework.data.jpa.repository.Query(
            "select c.cliente.id as clienteId,c.armazem.id as armazemId from ContratoCobranca c where c.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);
}
