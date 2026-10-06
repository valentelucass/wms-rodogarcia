package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.CargaInicial;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CargaInicialRepository extends JpaRepository<CargaInicial, Long> {
    Optional<CargaInicial> findByClienteIdAndArmazemIdAndReferencia(
            Long clienteId, Long armazemId, String referencia);

    Page<CargaInicial> findByClienteIdAndArmazemId(Long clienteId, Long armazemId, Pageable pagina);

    boolean existsByEntradaId(Long entradaId);

    @org.springframework.data.jpa.repository.Query(
            "select c from CargaInicial c where c.cliente.id=:clienteId and c.armazem.id=:armazemId and (:produtoId is null or c.produto.id=:produtoId) and (:referencia is null or c.referencia=:referencia) and (:situacao is null or c.situacao=:situacao)")
    Page<CargaInicial> consultar(
            Long clienteId,
            Long armazemId,
            Long produtoId,
            String referencia,
            br.com.rodogarcia.wms.models.SituacaoCargaInicial situacao,
            Pageable pagina);
}
