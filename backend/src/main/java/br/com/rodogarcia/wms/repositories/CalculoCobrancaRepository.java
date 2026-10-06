package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.CalculoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalculoCobrancaRepository extends JpaRepository<CalculoCobranca, Long> {
    org.springframework.data.domain.Page<CalculoCobranca> findByClienteIdAndArmazemId(
            Long clienteId, Long armazemId, org.springframework.data.domain.Pageable pagina);

    @org.springframework.data.jpa.repository.Query(
            "select c.cliente.id as clienteId,c.armazem.id as armazemId from CalculoCobranca c where c.id=:id")
    java.util.Optional<EscopoCobranca> buscarEscopo(Long id);

    @org.springframework.data.jpa.repository.Query(
            "select (count(c)>0) from CalculoCobranca c where c.cliente.id=:clienteId and c.armazem.id=:armazemId and c.periodoFim>:data and c.situacao='COMPLETO'")
    boolean contextoCalculadoApos(Long clienteId, Long armazemId, java.time.LocalDate data);

    @org.springframework.data.jpa.repository.Query(
            "select (count(m)>0) from MemoriaServico m where m.itemTabela.tabela.id=:tabelaId and m.calculo.periodoFim>:data")
    boolean tabelaUsadaApos(Long tabelaId, java.time.LocalDate data);
}
