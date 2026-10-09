package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import java.util.Collection;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaCadastroRepository extends JpaRepository<AuditoriaCadastro, Long> {
    /** Candidatos físicos; associação lógica e paginação são aplicadas antes da resposta. */
    Stream<AuditoriaCadastro> findByTipoInAndRegistroIdOrderByIdAsc(
            Collection<String> tipos, Long registroId);

    java.util.List<AuditoriaCadastro> findByTipoAndRegistroIdAndAcaoOrderByIdAsc(
            String tipo, Long registroId, String acao);

    Page<AuditoriaCadastro> findByTipoAndRegistroId(
            String tipo, Long registroId, Pageable pageable);
}
