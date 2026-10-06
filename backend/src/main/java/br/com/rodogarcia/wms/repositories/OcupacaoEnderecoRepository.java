package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OcupacaoEnderecoRepository extends JpaRepository<OcupacaoEndereco, Long> {
    boolean existsByEnderecoIdAndUnidadeIsNotNull(Long enderecoId);

    Optional<OcupacaoEndereco> findByEnderecoId(Long enderecoId);

    @Query(
            "select o.endereco.id from OcupacaoEndereco o where o.unidade.id = :unidadeId order by o.endereco.id")
    List<Long> buscarEnderecos(Long unidadeId);

    @Query(
            "select o from OcupacaoEndereco o join fetch o.endereco where o.unidade.id in :ids order by o.endereco.id")
    List<OcupacaoEndereco> buscarDasUnidades(List<Long> ids);
}
