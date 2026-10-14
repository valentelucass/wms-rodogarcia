package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.Endereco;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
    @Query(
            """
        select count(e) from Endereco e where e.id=:id and (
          exists(select o.id from OcupacaoEndereco o where o.endereco.id=e.id and o.unidade is not null) or
          exists(select c.id from ConjuntoPosicoes c where (c.enderecoA.id=e.id or c.enderecoB.id=e.id)
            and c.situacao=br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO))
        """)
    long contarUsoAtual(Long id);

    java.util.List<Endereco> findByArmazemIdOrderByIdAsc(Long armazemId);

    Page<Endereco> findByArmazemId(Long armazemId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Endereco c where c.id = :id")
    Optional<Endereco> buscarParaAtualizar(@Param("id") Long id);

    @Query("select e.armazem.id from Endereco e where e.id = :id")
    Optional<Long> buscarArmazemId(Long id);
}
