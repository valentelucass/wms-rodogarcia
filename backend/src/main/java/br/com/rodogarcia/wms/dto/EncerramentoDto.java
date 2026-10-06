package br.com.rodogarcia.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class EncerramentoDto {
    private EncerramentoDto() {}

    public enum Tipo {
        CLIENTE,
        ARMAZEM,
        PRODUTO,
        EMBALAGEM,
        ENDERECO,
        CONJUNTO_POSICOES,
        SERVICO_COBRANCA
    }

    public record Confirmar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Impedimento(String codigo, String recurso, Long id, String detalhe) {}

    public record Resultado(
            Tipo tipo, Long id, long versao, String situacao, List<Impedimento> impedimentos) {}

    public record Unidade(
            @NotNull @Positive Long unidadeId,
            @NotNull @Min(0) Long versao,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade) {}

    public record Remanescente(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) String referencia,
            @NotNull @Size(min = 1, max = 10000) List<@NotNull @Valid Unidade> unidades,
            @Positive Long cargaInicialId,
            @NotNull @Size(max = 10000) List<@NotBlank String> etiquetas,
            @Size(min = 5, max = 500) String justificativaFifo,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record EncerrarVigencia(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull LocalDate corte,
            @NotNull @Valid FechamentoCobrancaDto.Resolucao resolucao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Vigencia(String tipo, Long id, long versao, LocalDate inicio, LocalDate fim) {}
}
