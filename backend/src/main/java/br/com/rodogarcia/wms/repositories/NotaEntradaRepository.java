package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.NotaEntrada;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotaEntradaRepository extends JpaRepository<NotaEntrada, Long> {
    List<NotaEntrada> findByPedidoIdOrderById(Long pedidoId);

    Optional<NotaEntrada> findByEmitenteAndSerieAndNumero(String emitente, int serie, long numero);

    boolean existsByChaveAcesso(String chave);

    Optional<NotaEntrada> findByChaveAcesso(String chave);

    long countByPedidoId(Long pedidoId);
}
