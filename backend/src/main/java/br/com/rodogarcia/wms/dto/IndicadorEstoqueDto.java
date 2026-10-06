package br.com.rodogarcia.wms.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class IndicadorEstoqueDto {
    private IndicadorEstoqueDto() {}

    public record ConfigurarAviso(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull @Min(0) Long versao,
            @NotNull @Min(0) @Max(36500) Integer diasAntecedencia,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Configuracao(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            Integer diasAntecedencia,
            boolean configurada) {}

    public record Aviso(Long unidadeId, String codigo, LocalDate validade, String situacao) {}

    public record ValorOrigem(
            Long entradaId,
            Long notaId,
            Long itemNotaId,
            BigDecimal quantidade,
            BigDecimal valorConhecido,
            boolean completo,
            List<Long> unidades) {}

    public record Resultado(
            Long produtoId,
            String sku,
            EstoqueDto.Saldo saldo,
            BigDecimal quantidadeEmEstagio,
            BigDecimal quantidadeConferenciaPendente,
            BigDecimal observadoContagem,
            BigDecimal diferencaContagem,
            boolean valorConsultado,
            BigDecimal valorConhecido,
            BigDecimal valorExato,
            List<ValorOrigem> origens,
            List<String> pendencias,
            List<Aviso> avisos) {}
}
