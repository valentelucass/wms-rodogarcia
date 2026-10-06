package br.com.rodogarcia.wms.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
        List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {
    public static <E, T> PaginaResponse<T> de(Page<E> page, Function<E, T> converter) {
        return new PaginaResponse<>(
                page.getContent().stream().map(converter).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
