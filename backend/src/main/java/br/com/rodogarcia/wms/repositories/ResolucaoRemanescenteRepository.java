package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ResolucaoRemanescente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResolucaoRemanescenteRepository
        extends JpaRepository<ResolucaoRemanescente, Long> {
    Optional<ResolucaoRemanescente> findByPedidoSaidaId(Long pedidoId);
}
