package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class DashboardDto {
    private DashboardDto() {}

    public record Area(
            @NotNull TipoEndereco tipo,
            long posicoesCliente,
            Long capacidadeAtiva,
            Long livresArmazem) {}

    public record Fila(@NotNull SituacaoPedidoSaida situacao, long pedidos) {}

    public record Produto(
            @NotNull Long produtoId,
            @NotNull String sku,
            @NotNull String unidadeMedida,
            @NotNull BigDecimal fisicoTotal,
            @NotNull BigDecimal disponivel,
            @NotNull BigDecimal reservado,
            @NotNull BigDecimal indisponivel,
            @NotNull BigDecimal pendenteUnitizacao) {}

    public record Resumo(
            @NotNull Long clienteId,
            @NotNull Long armazemId,
            @NotNull Instant consultadoEm,
            @NotNull String fuso,
            long posicoesCliente,
            long unidadesDisponiveis,
            long pedidosAbertos,
            Long unidadesComAviso,
            Integer antecedenciaValidade,
            boolean visaoArmazem,
            @NotNull List<Area> areas,
            @NotNull List<Fila> fila,
            @NotNull PaginaResponse<Produto> produtos) {}
}
