package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.CoberturaDocumentoSaida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CoberturaDocumentoSaidaRepository
        extends JpaRepository<CoberturaDocumentoSaida, Long> {
    @Query(
            "select c from CoberturaDocumentoSaida c where c.documento.pedido.id=:pedidoId order by c.id")
    List<CoberturaDocumentoSaida> buscarDoPedido(Long pedidoId);
}
