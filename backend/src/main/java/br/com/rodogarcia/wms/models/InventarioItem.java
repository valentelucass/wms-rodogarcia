package br.com.rodogarcia.wms.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InventarioItem(
        Base base,
        boolean prevista,
        BigDecimal contado,
        Long enderecoLido,
        String posicaoLida,
        Instant lidoEm,
        String usuario) {
    public record Posicao(Long id, String codigo) {}

    public record Base(
            Long unidadeId,
            String codigoUnidade,
            long versaoUnidade,
            Long produtoId,
            String sku,
            String descricao,
            String unidadeMedida,
            int precisao,
            BigDecimal quantidade,
            List<Posicao> posicoes) {}
}
