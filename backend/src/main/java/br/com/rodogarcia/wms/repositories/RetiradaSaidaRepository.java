package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.RetiradaSaida;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetiradaSaidaRepository extends JpaRepository<RetiradaSaida, Long> {
    Optional<RetiradaSaida> findByPedidoId(Long pedidoId);

    @org.springframework.data.jpa.repository.Query(
            "select r from RetiradaSaida r where r.pedido.cliente.id=:clienteId and r.pedido.armazem.id=:armazemId and not exists(select f.id from FatoServico f where f.servico.id=:servicoId and f.pedidoSaida.id=r.pedido.id)")
    org.springframework.data.domain.Page<br.com.rodogarcia.wms.models.RetiradaSaida>
            candidatasServico(
                    Long clienteId,
                    Long armazemId,
                    Long servicoId,
                    org.springframework.data.domain.Pageable pagina);
}
