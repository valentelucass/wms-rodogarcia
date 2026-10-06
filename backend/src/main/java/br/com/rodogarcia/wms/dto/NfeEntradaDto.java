package br.com.rodogarcia.wms.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NfeEntradaDto(
        String emitente,
        int serie,
        long numero,
        LocalDate emissao,
        String chaveAcesso,
        List<Item> itens) {
    public record Item(
            int numeroItem,
            String sku,
            String unidade,
            BigDecimal quantidade,
            BigDecimal valorMercadoria) {}
}
