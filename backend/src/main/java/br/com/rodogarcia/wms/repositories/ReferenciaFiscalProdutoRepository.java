package br.com.rodogarcia.wms.repositories;

import br.com.rodogarcia.wms.models.ReferenciaFiscalProduto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferenciaFiscalProdutoRepository
        extends JpaRepository<ReferenciaFiscalProduto, Long> {
    java.util.Optional<ReferenciaFiscalProduto> findByProdutoIdAndArmazemIdAndOperacao(
            Long produtoId, Long armazemId, String operacao);

    java.util.List<ReferenciaFiscalProduto> findByProdutoIdOrderByIdAsc(Long produtoId);
}
