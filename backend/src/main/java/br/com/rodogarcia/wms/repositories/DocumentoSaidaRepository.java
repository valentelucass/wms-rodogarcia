package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.DocumentoSaida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoSaidaRepository extends JpaRepository<DocumentoSaida, Long> {
    List<DocumentoSaida> findByPedidoIdOrderById(Long pedidoId);
}
