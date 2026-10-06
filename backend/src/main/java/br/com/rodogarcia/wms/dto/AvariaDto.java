package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.dto.EstoqueDto.Destino;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AvariaDto {
    private AvariaDto() {}

    public record Registrar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versaoUnidade,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade,
            @NotNull Instant ocorridaEm,
            @Positive Long conjuntoId,
            @NotNull @Size(min = 1, max = 2) List<@NotNull @Valid Destino> destinos,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Registrar {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record Reconhecer(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Pattern(regexp = "RODOGARCIA|PROPRIETARIO|TERCEIRO") String responsabilidade,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Reparar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Min(0) Long versaoUnidade,
            @Positive Long conjuntoId,
            @NotNull @Size(min = 1, max = 2) List<@NotNull @Valid Destino> destinos,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Reparar {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record Ocorrencia(
            Long id,
            long versao,
            Long unidadeId,
            BigDecimal quantidade,
            BigDecimal quantidadeBase,
            BigDecimal equivalenciaBase,
            String cicloId,
            Instant ocorridaEm,
            Instant registradaEm,
            String responsabilidade,
            String relato,
            Instant reconhecidaEm,
            String validadaPor,
            String tratativa,
            Instant resolvidaEm,
            BigDecimal proporcaoSuspensa,
            Instant inicioSuspensao) {}

    public record Confirmacao(UUID operacaoId, Ocorrencia avaria, EstoqueDto.Unidade estoque) {}
}
