package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto.NovaUnidade;
import br.com.rodogarcia.wms.models.SituacaoCargaInicial;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CargaInicialDto {
    private CargaInicialDto() {}

    public enum FiltroSituacao {
        PENDENTE,
        PREPARADA,
        REGULARIZADA,
        CANCELADA
    }

    public record Dados(
            @Positive Long entradaExistenteId,
            @Size(max = 40) String referenciaPedido,
            @Valid PedidoEntradaDto.NotaManual nota,
            Instant chegadaReal,
            Instant dataFifo,
            @Size(max = 60) String lote,
            LocalDate validade,
            @DecimalMin("0") @Digits(integer = 13, fraction = 6) BigDecimal quantidadeBoa,
            @DecimalMin("0") @Digits(integer = 13, fraction = 6) BigDecimal quantidadeAvariada,
            @Size(max = 100) List<@NotNull @Valid NovaUnidade> unidades,
            @Size(max = 500) String fonte) {}

    public record Criar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull @Positive Long produtoId,
            @NotBlank @Size(max = 100) String referencia,
            @Size(max = 200) String etiquetaFornecida,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade,
            @NotNull @Valid Dados dados,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Revisar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Valid Dados dados,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Confirmar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Min(1) Integer revisao,
            @NotBlank @Size(min = 64, max = 64) String conteudoHash,
            @NotBlank @Size(max = 200) String leitura,
            @NotNull @Size(max = 10000) List<@NotBlank String> etiquetasUnidades,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Cancelar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record ResolverCancelamento(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @Positive Long pedidoResolucaoId,
            @NotNull @Size(max = 10000) List<@Positive Long> contagensIds,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Revisao(int numero, String conteudoHash, Dados dados) {}

    public record Resultado(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            Long produtoId,
            String referencia,
            String etiquetaFornecida,
            BigDecimal quantidadeEstagio,
            SituacaoCargaInicial situacao,
            Revisao revisao,
            Long entradaId,
            List<String> etiquetas,
            List<String> pendencias) {}
}
