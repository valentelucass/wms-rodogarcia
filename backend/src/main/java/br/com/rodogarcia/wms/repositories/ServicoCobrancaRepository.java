package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ServicoCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServicoCobrancaRepository extends JpaRepository<ServicoCobranca, Long> {
    /** Leitura serializada; chamada somente na transação de criação, antes do INSERT. */
    @Query(
            value =
                    """
            SELECT COALESCE(MAX(TRY_CONVERT(bigint, SUBSTRING(codigo, 5, 100))), 0)
            FROM wms.servico_cobranca WITH (TABLOCKX, HOLDLOCK)
            WHERE codigo LIKE 'SRV-%'
              AND TRY_CONVERT(bigint, SUBSTRING(codigo, 5, 100)) >= 0
            """,
            nativeQuery = true)
    long ultimoCodigoInterno();

    boolean existsByCodigo(String codigo);

    @org.springframework.data.jpa.repository.Lock(
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from ServicoCobranca s where s.id=:id")
    java.util.Optional<ServicoCobranca> buscarParaAtualizar(Long id);
}
