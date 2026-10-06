package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class PedidoSaidaDto {
    private PedidoSaidaDto() {}

    public record ItemCriar(
            @NotNull @Positive Long produtoId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade) {}

    public record Criar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) String referencia,
            @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid ItemCriar> itens,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Selecao(
            @NotNull @Positive Long unidadeId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade) {}

    public record Justificar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Size(min = 1, max = 500) List<@NotNull @Valid Selecao> selecoes,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Reservar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            UUID justificativaId,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Comando(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Item(
            Long id, Long produtoId, String sku, String unidadeMedida, BigDecimal quantidade) {}

    public record Reserva(
            Long id,
            Long itemId,
            Long unidadeId,
            String codigoUnidade,
            Long notaOrigemId,
            String lote,
            Instant dataFifo,
            BigDecimal quantidade,
            SituacaoReservaSaida situacao,
            Instant criadaEm,
            Instant encerradaEm) {}

    public record Detalhe(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            String referencia,
            SituacaoPedidoSaida situacao,
            Instant criadoEm,
            Instant alteradoEm,
            List<Item> itens,
            List<Reserva> reservas,
            List<Long> unidadesImpedidas,
            boolean podeProsseguir) {}

    public record Confirmacao(
            UUID operacaoId,
            Detalhe pedido,
            List<Selecao> selecoes,
            boolean excecaoFifo,
            String justificadaPor,
            String justificativa) {}

    public record Sugestao(long versao, List<Selecao> selecoes) {}
}
