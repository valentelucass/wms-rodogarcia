package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.TabelaCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TabelaCobrancaRepository extends JpaRepository<TabelaCobranca, Long> {
    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TabelaCobranca t where t.id=:id")
    java.util.Optional<TabelaCobranca> buscarParaAtualizar(Long id);

    /** Leitura serializada; chamada somente na transação de criação, antes do INSERT. */
    @Query(
            value =
                    """
            SELECT COALESCE(MAX(TRY_CONVERT(bigint, SUBSTRING(codigo, 5, 100))), 0)
            FROM wms.tabela_cobranca WITH (TABLOCKX, HOLDLOCK)
            WHERE codigo LIKE 'TAB-%'
              AND TRY_CONVERT(bigint, SUBSTRING(codigo, 5, 100)) >= 0
            """,
            nativeQuery = true)
    long ultimoCodigoInterno();

    boolean existsByArmazemIdAndCodigo(Long armazemId, String codigo);

    java.util.List<TabelaCobranca> findByArmazemIdOrderByIdAsc(Long armazemId);

    @org.springframework.data.jpa.repository.Query(
            "select t.armazem.id from TabelaCobranca t where t.id=:id")
    java.util.Optional<Long> buscarArmazemId(Long id);
}
