package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.SituacaoRevisaoContagem;
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

public final class ContagemDto {
    private ContagemDto() {}

    public enum FiltroSituacao {
        PENDENTE,
        PENDENTE_RESERVA,
        RECONCILIADA,
        SUBSTITUIDA,
        APLICADA
    }

    public record Contar(
            @NotNull UUID operacaoId,
            @NotNull UUID codigoUnidade,
            @NotNull @Min(0) Long versaoUnidade,
            @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 6) BigDecimal contado,
            @NotNull Instant observadoEm,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record DeltaOrigem(
            @NotNull @Positive Long entradaId,
            @NotNull @Digits(integer = 13, fraction = 6) BigDecimal delta) {}

    public record Aplicar(
            @NotNull UUID operacaoId,
            @NotNull @Min(1) Integer revisao,
            @NotNull @Min(0) Long versaoUnidade,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @NotBlank @Size(min = 5, max = 500) String causa,
            @NotBlank @Size(min = 5, max = 500) String destino,
            @NotBlank @Size(min = 5, max = 500) String comprovacao,
            @NotNull @Size(min = 1, max = 1000) List<@NotNull @Valid DeltaOrigem> origens) {}

    public record Origem(Long entradaId, Long itemNotaId, BigDecimal quantidade) {}

    public record Resultado(
            Long id,
            long versao,
            Long unidadeId,
            String codigoUnidade,
            int revisao,
            BigDecimal esperado,
            BigDecimal contado,
            BigDecimal diferenca,
            BigDecimal reservado,
            Instant observadoEm,
            SituacaoRevisaoContagem situacao,
            boolean impedimento,
            List<Origem> origens,
            String efeitoJson) {}
}
