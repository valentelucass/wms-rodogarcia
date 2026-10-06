package br.com.rodogarcia.wms.dto;

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

public final class FatoServicoDto {
    private FatoServicoDto() {}

    public record Cota(
            @NotNull @Positive Long notaId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal cota) {}

    public record Registrar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull @Positive Long servicoId,
            @NotNull @Pattern(regexp = "MANUAL|SUGESTAO") String origem,
            @Positive Long unidadeId,
            @Positive Long pedidoEntradaId,
            @Positive Long pedidoSaidaId,
            @Positive Long produtoId,
            @Size(max = 80) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String referenciaExecucao,
            Instant executadoEm,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade,
            @NotNull @Size(max = 40) String categoria,
            @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal valorBase,
            @NotNull @Size(max = 500) List<@NotNull @Valid Cota> cotas,
            @NotBlank @Size(min = 5, max = 200) String criterioRateio,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record Anular(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record Fato(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            Long servicoId,
            String chaveFato,
            String origem,
            Long unidadeId,
            Long pedidoEntradaId,
            Long pedidoSaidaId,
            Long produtoId,
            String referenciaExecucao,
            Instant executadoEm,
            BigDecimal quantidade,
            String categoria,
            BigDecimal valorBase,
            String criterioRateio,
            String situacao,
            List<Cota> cotas) {}

    public record Sugestao(
            Long servicoId,
            Long unidadeId,
            Long pedidoSaidaId,
            Long produtoId,
            String chaveFato,
            Instant executadoEm,
            BigDecimal quantidade,
            String categoria,
            String pendencia) {}

    public record Marco(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long fatoPermanenciaId,
            @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidadeAfetada,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record MarcoResposta(
            Long id,
            Long avariaId,
            Long fatoPermanenciaId,
            BigDecimal quantidadeAfetada,
            BigDecimal quantidadeBase,
            BigDecimal equivalenciaBase,
            Instant validadoEm,
            String usuario,
            String motivo) {}
}
