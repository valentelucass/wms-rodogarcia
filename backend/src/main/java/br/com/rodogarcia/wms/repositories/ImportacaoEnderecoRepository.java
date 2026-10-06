package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ImportacaoEndereco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportacaoEnderecoRepository extends JpaRepository<ImportacaoEndereco, Long> {
    @org.springframework.data.jpa.repository.Query(
            "select i.armazem.id from ImportacaoEndereco i where i.id=:id")
    java.util.Optional<Long> buscarArmazemId(Long id);
}
