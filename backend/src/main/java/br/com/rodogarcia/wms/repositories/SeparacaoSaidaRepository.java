package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.SeparacaoSaida;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeparacaoSaidaRepository extends JpaRepository<SeparacaoSaida, Long> {
    Optional<SeparacaoSaida> findByReservaId(Long reservaId);

    @Query("select s from SeparacaoSaida s where s.reserva.item.pedido.id=:pedidoId order by s.id")
    List<SeparacaoSaida> buscarDoPedido(Long pedidoId);
}
