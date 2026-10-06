package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.OperacaoSaida;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacaoSaidaRepository extends JpaRepository<OperacaoSaida, Long> {
    Optional<OperacaoSaida> findByPedidoIdAndOperacaoId(Long pedidoId, String operacaoId);
}
