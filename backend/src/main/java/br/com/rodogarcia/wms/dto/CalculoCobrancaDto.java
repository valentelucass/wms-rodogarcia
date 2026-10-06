package br.com.rodogarcia.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CalculoCobrancaDto {
    private CalculoCobrancaDto() {}

    public record Calcular(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull LocalDate periodoInicio,
            @NotNull LocalDate periodoFim,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record Pendencia(
            String codigo, LocalDate data, Long unidadeId, Long fatoId, String detalhe) {}

    public record Contribuicao(
            Long unidadeId,
            String codigo,
            String categoria,
            BigDecimal quantidade,
            BigDecimal equivalenciaFisica,
            BigDecimal quantidadeAvariada,
            BigDecimal equivalenciaSuspensa,
            BigDecimal equivalenciaLiquida,
            BigDecimal valorEstoque,
            List<Long> avarias,
            List<Long> fatos,
            List<OrigemValor> origens) {}

    public record OrigemValor(
            Long entradaId,
            Long notaId,
            Long itemNotaId,
            BigDecimal quantidade,
            BigDecimal quantidadeItem,
            BigDecimal valorItem,
            BigDecimal valorUnitario,
            BigDecimal valorHistorico) {}

    public record ValorEntrada(
            Long entradaId,
            Long notaId,
            Long itemNotaId,
            BigDecimal quantidadeBoa,
            BigDecimal quantidadeAvariada,
            BigDecimal valorHistorico) {}

    public record SegmentoValor(
            Instant inicio,
            Instant fim,
            BigDecimal total,
            List<Contribuicao> unidades,
            List<ValorEntrada> entradasNaoUnitizadas) {}

    public record Segmento(Instant inicio, Instant fim, List<Contribuicao> contribuicoes) {}

    public record RegraDiaria(
            Long tabelaId,
            Long itemTabelaId,
            Long servicoId,
            String categoria,
            BigDecimal pico,
            BigDecimal tarifa,
            BigDecimal valor,
            Instant instantePico) {}

    public record Diaria(
            LocalDate data,
            Long tabelaId,
            Long itemTabelaId,
            BigDecimal picoCobravel,
            BigDecimal equivalenciaSuspensa,
            BigDecimal valorEstoque,
            BigDecimal tarifa,
            BigDecimal valor,
            List<RegraDiaria> regras,
            List<Segmento> segmentos,
            List<SegmentoValor> intervalosValor) {}

    public record Parcela(Long notaId, BigDecimal cota, BigDecimal valor) {}

    public record Servico(
            Long fatoId,
            Long servicoId,
            String chaveFato,
            Instant executadoEm,
            Long tabelaId,
            Long itemTabelaId,
            BigDecimal quantidade,
            BigDecimal preco,
            BigDecimal percentual,
            BigDecimal valorBase,
            BigDecimal valor,
            BigDecimal antesArredondamento,
            List<Parcela> parcelas) {}

    public record Ajustes(
            LocalDate cicloInicio,
            LocalDate cicloFim,
            long diasIncluidos,
            long diasNominais,
            List<Long> abrangenciaMinimo,
            BigDecimal subtotalElegivel,
            BigDecimal minimoAplicavel,
            BigDecimal minimoComplemento,
            String grisBase,
            String grisPeriodicidade,
            String grisProporcao,
            BigDecimal grisPercentual,
            BigDecimal baseGris,
            BigDecimal valorGris,
            String regraGris) {}

    public record Memoria(List<Diaria> diarias, List<Servico> servicos, Ajustes ajustes) {}

    public record Resultado(
            Long id,
            Long clienteId,
            Long armazemId,
            Long contratoId,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            String fuso,
            String moeda,
            String situacao,
            int regraVersao,
            String entradasHash,
            BigDecimal subtotalConhecido,
            BigDecimal minimoCalculado,
            BigDecimal grisCalculado,
            BigDecimal total,
            Instant calculadoEm,
            List<Pendencia> pendencias,
            Memoria memoria) {}
}
