package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ItemChegada;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ItemChegadaRepository extends JpaRepository<ItemChegada, Long> {
    interface QuantidadeRecebida {
        Long getItemId();

        BigDecimal getBoa();

        BigDecimal getAvariada();

        Instant getPrimeiraChegada();
    }

    @Query(
            "select i.itemNota.id as itemId, sum(i.quantidadeBoa) as boa, sum(i.quantidadeAvariada) as avariada, min(i.chegada.chegouEm) as primeiraChegada from ItemChegada i where i.chegada.pedido.id = :pedidoId and i.chegada.estornadaEm is null group by i.itemNota.id")
    List<QuantidadeRecebida> resumir(Long pedidoId);

    @Query(
            "select i from ItemChegada i join fetch i.chegada c join fetch i.itemNota n join fetch n.nota where c.pedido.id = :pedidoId and c.estornadaEm is null order by i.id")
    List<ItemChegada> buscarAtivos(Long pedidoId);

    @Query(
            "select i from ItemChegada i join fetch i.itemNota where i.chegada.id in :ids order by i.id")
    List<ItemChegada> buscarDasChegadas(List<Long> ids);
}
