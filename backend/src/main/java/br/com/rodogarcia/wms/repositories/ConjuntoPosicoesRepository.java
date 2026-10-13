package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ConjuntoPosicoesRepository extends JpaRepository<ConjuntoPosicoes, Long> {
    /** Leitura serializada; chamada somente na transação de criação, antes do INSERT. */
    @Query(
            value =
                    """
            SELECT COALESCE(MAX(TRY_CONVERT(bigint, SUBSTRING(codigo, 5, 100))), 0)
            FROM wms.conjunto_posicoes WITH (TABLOCKX, HOLDLOCK)
            WHERE codigo LIKE 'CON-%'
              AND TRY_CONVERT(bigint, SUBSTRING(codigo, 5, 100)) >= 0
            """,
            nativeQuery = true)
    long ultimoCodigoInterno();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConjuntoPosicoes c where c.id = :id")
    Optional<ConjuntoPosicoes> buscarParaAtualizar(Long id);

    @Query("select c.armazem.id from ConjuntoPosicoes c where c.id = :id")
    Optional<Long> buscarArmazemId(Long id);

    @Query(
            "select count(c) from ConjuntoPosicoes c where (c.enderecoA.id = :id or c.enderecoB.id = :id) and c.situacao = br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO")
    long contarAtivosDoEndereco(Long id);

    @EntityGraph(attributePaths = {"armazem", "enderecoA", "enderecoB"})
    Page<ConjuntoPosicoes> findByArmazemId(Long armazemId, Pageable pageable);
}
