package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.FatoServico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FatoServicoRepository extends JpaRepository<FatoServico, Long> {
    java.util.Optional<FatoServico> findByClienteIdAndArmazemIdAndServicoIdAndChaveFato(
            Long clienteId, Long armazemId, Long servicoId, String chaveFato);

    java.util.List<FatoServico> findByClienteIdAndArmazemIdOrderByExecutadoEmAscIdAsc(
            Long clienteId, Long armazemId);

    @org.springframework.data.jpa.repository.Query(
            "select c.cliente.id as clienteId,c.armazem.id as armazemId from FatoServico c where c.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);

    org.springframework.data.domain.Page<FatoServico> findByClienteIdAndArmazemId(
            Long clienteId, Long armazemId, org.springframework.data.domain.Pageable pagina);
}
