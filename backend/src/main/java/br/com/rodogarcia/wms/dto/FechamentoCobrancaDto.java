package br.com.rodogarcia.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FechamentoCobrancaDto {
    private FechamentoCobrancaDto() {}

    public record Resolucao(
            @NotBlank @Pattern(regexp = "PEDIDO_ENTRADA|PEDIDO_SAIDA|FECHAMENTO") String tipo,
            @NotNull @Positive Long compromissoId) {}

    public record Preparar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long calculoId,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Decidir(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numero,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Reabrir(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numero,
            @NotNull @Positive Long calculoId,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Entregar(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numero,
            @Positive int layoutVersao,
            @NotBlank @Pattern(regexp = "[a-fA-F0-9]{64}") String arquivoHash,
            @NotBlank @Size(max = 200) String destinoReferencia,
            @NotNull Instant entregueEm,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Confirmar(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numero,
            @Positive Long entregaId,
            @NotBlank @Size(max = 500) String fonte,
            @NotBlank @Size(max = 200) String confirmadaPor,
            @NotNull Instant confirmadaEm,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Nfse(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numero,
            @NotBlank @Size(max = 20) String emissorDocumento,
            @NotBlank @Size(max = 200) String referenciaExterna,
            @Size(max = 80) String numeroDocumento,
            @Size(max = 20) String serie,
            @NotNull Instant emitidaEm,
            @NotBlank @Size(max = 500) String fonte,
            @NotBlank @Size(max = 200) String conferidaPor,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Resolver(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numero,
            @NotBlank @Size(max = 200) String referenciaExterna,
            @NotBlank @Size(max = 500) String fonte,
            @NotBlank @Size(max = 200) String confirmadaPor,
            @NotNull Instant confirmadaEm,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao) {}

    public record Ajustar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long origemVersaoId,
            @NotNull @Positive Long destinoFechamentoId,
            @NotNull @PositiveOrZero Long versaoDestino,
            @NotNull @Positive Long calculoCorrigidoId,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @NotBlank @Size(max = 500) String evidencia,
            @Valid Resolucao resolucao) {}

    public record Fechamento(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            Long contratoId,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            String situacao,
            int versaoAtual,
            Instant criadoEm,
            Instant alteradoEm) {}

    public record Versao(
            Long id,
            Long fechamentoId,
            int numero,
            Long calculoId,
            String situacao,
            String estadoExterno,
            String conteudoHash,
            BigDecimal saldo,
            String natureza,
            Instant criadaEm,
            Instant decididaEm,
            String decisor,
            String motivoDecisao,
            List<Entrega> entregas,
            List<Confirmacao> confirmacoes,
            List<Documento> nfse,
            ResolucaoSaldo resolucaoFinanceira,
            List<Ajuste> ajustes) {}

    public record DocumentoConferido(
            @NotNull @Positive Long referenciaId,
            @NotBlank @Pattern(regexp = "MANTIDO|CANCELAMENTO_COMPROVADO") String situacao) {}

    public record Tratar(
            @NotNull UUID operacaoId,
            @NotNull @PositiveOrZero Long versao,
            @Positive int numeroResultado,
            @NotBlank @Pattern(regexp = "EMITIDO|NAO_EMITIDO_CONFIRMADO") String resultado,
            @NotNull @Size(min = 1) List<@NotNull @Valid DocumentoConferido> referencias,
            @NotBlank @Size(max = 500) String fonte,
            @NotBlank @Size(max = 200) String conferidaPor,
            @NotNull Instant conferidaEm,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid Resolucao resolucao,
            @Positive Long destinoAjustesId,
            @PositiveOrZero Long versaoDestinoAjustes,
            @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    @Valid
                    RegularizacaoOrigem regularizacaoOrigem) {}

    public record RegularizacaoOrigem(
            @NotNull @Size(min = 1) List<@NotNull @Positive Long> ajustesDependentesIds,
            @NotNull @Positive Long destinoFechamentoId,
            @NotNull @PositiveOrZero Long versaoDestino,
            @NotNull BigDecimal diferencaEsperada) {}

    public record Tratativa(
            Long id,
            Long fechamentoId,
            Long versaoResultadoId,
            String resultado,
            List<DocumentoConferido> referencias,
            String fonte,
            String conferidaPor,
            Instant conferidaEm,
            Instant registradaEm,
            String usuario,
            String motivo,
            Long versaoBaseAnteriorId,
            List<Ajuste> dependenciasOrigem) {}

    public record Entrega(
            Long id,
            int sequencia,
            int layoutVersao,
            String arquivoHash,
            String destinoReferencia,
            Instant entregueEm,
            Instant registradaEm,
            String usuario,
            String motivo) {}

    public record Confirmacao(
            Long id,
            Long entregaId,
            String situacao,
            String fonte,
            String confirmadaPor,
            Instant confirmadaEm,
            Instant registradaEm,
            String usuario,
            String motivo) {}

    public record Documento(
            Long id,
            String emissorDocumento,
            String referenciaExterna,
            String numero,
            String serie,
            Instant emitidaEm,
            Instant registradaEm,
            String fonte,
            String conferidaPor,
            String usuario,
            String motivo) {}

    public record ResolucaoSaldo(
            Long id,
            String tipo,
            String referenciaExterna,
            String fonte,
            String confirmadaPor,
            Instant confirmadaEm,
            Instant registradaEm,
            String usuario,
            String motivo) {}

    public record Ajuste(
            Long id,
            Long origemVersaoId,
            Long destinoFechamentoId,
            Long calculoBaseId,
            Long calculoCorrigidoId,
            String hashCorrecao,
            BigDecimal valorBase,
            BigDecimal valorCorrigido,
            BigDecimal diferenca,
            String situacao,
            Long aplicadoVersaoId,
            Instant aplicadoEm,
            String motivo,
            String evidencia,
            String usuario,
            Instant registradoEm,
            @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    String tipo,
            @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    Long tratativaOrigemId) {}

    public record ConfirmacaoComando(Fechamento fechamento, Versao versao) {}

    public record Identificacao(
            Long id,
            String codigo,
            String nome,
            String documentoFiscal,
            FiscalCadastroDto.Dados dados) {}

    public record Ciclo(
            String modalidade,
            LocalDate ancora,
            Integer diaNominal,
            Integer duracaoDias,
            LocalDate cicloInicio,
            LocalDate cicloFim,
            long diasNominais) {}

    public record Demonstrativo(
            int layoutVersao,
            Long fechamentoId,
            int numero,
            Identificacao cliente,
            Identificacao armazem,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            Ciclo ciclo,
            CalculoCobrancaDto.Resultado calculo,
            List<Ajuste> ajustes,
            BigDecimal saldo,
            String natureza) {}
}
