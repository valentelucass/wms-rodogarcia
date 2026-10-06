package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.FatoPermanencia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FatoPermanenciaRepository extends JpaRepository<FatoPermanencia, Long> {
    List<FatoPermanencia> findByUnidadeIdOrderByOcorridaEmAscIdAsc(Long unidadeId);

    @Query(
            "select f from FatoPermanencia f where f.unidade.reservaSaida.id=:pedidoId or exists (select r.id from ReservaSaida r where r.item.pedido.id=:pedidoId and r.unidade.id=f.unidade.id) order by f.ocorridaEm,f.id")
    List<FatoPermanencia> buscarDoPedido(Long pedidoId);
}
