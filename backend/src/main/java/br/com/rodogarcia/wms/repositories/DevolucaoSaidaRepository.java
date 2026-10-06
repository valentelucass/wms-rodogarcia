package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.DevolucaoSaida;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DevolucaoSaidaRepository extends JpaRepository<DevolucaoSaida, Long> {
    @Query(
            "select d from DevolucaoSaida d where d.baixa.retirada.pedido.id=:pedidoId order by d.id")
    List<DevolucaoSaida> buscarDoPedido(Long pedidoId);

    @Query("select sum(d.quantidade) from DevolucaoSaida d where d.baixa.id=:baixaId")
    BigDecimal somarDaBaixa(Long baixaId);
}
