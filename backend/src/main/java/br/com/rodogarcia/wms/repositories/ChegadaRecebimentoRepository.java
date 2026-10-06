package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChegadaRecebimentoRepository extends JpaRepository<ChegadaRecebimento, Long> {
    Optional<ChegadaRecebimento> findByPedidoIdAndOperacaoId(Long pedidoId, String operacaoId);

    Optional<ChegadaRecebimento> findByIdAndPedidoId(Long id, Long pedidoId);

    Page<ChegadaRecebimento> findByPedidoId(Long pedidoId, Pageable pageable);

    long countByPedidoId(Long pedidoId);

    java.util.List<ChegadaRecebimento> findByPedidoIdOrderByIdAsc(Long pedidoId);
}
