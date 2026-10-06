package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.LinhaContingencia;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LinhaContingenciaRepository extends JpaRepository<LinhaContingencia, Long> {
    Optional<LinhaContingencia> findByIdentidadeFato(String identidade);

    java.util.List<LinhaContingencia> findByClienteIdAndArmazemIdAndSituacao(
            Long clienteId,
            Long armazemId,
            br.com.rodogarcia.wms.models.SituacaoContingencia situacao);

    Page<LinhaContingencia> findByClienteIdAndArmazemId(
            Long clienteId, Long armazemId, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LinhaContingencia l where l.id=:id")
    Optional<LinhaContingencia> buscarParaAtualizar(Long id);

    @Query(
            "select l from LinhaContingencia l where l.cliente.id=:clienteId and l.armazem.id=:armazemId and (:identidade is null or l.identidadeFato=:identidade) and (:tipo is null or l.tipo=:tipo) and (:situacao is null or l.situacao=:situacao) and (:pendencia is null or l.pendencia=:pendencia)")
    Page<LinhaContingencia> consultar(
            Long clienteId,
            Long armazemId,
            String identidade,
            br.com.rodogarcia.wms.models.TipoContingencia tipo,
            br.com.rodogarcia.wms.models.SituacaoContingencia situacao,
            String pendencia,
            Pageable pagina);
}
