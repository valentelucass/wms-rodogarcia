package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
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

public final class UnidadeLogisticaDto {
    private UnidadeLogisticaDto() {}

    public record NovaUnidade(
            @NotNull @Positive Long embalagemId,
            @NotNull TipoUnidadeLogistica tipo,
            @NotNull CondicaoMercadoria condicao,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade) {}

    public record Unitizar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versaoPedido,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid NovaUnidade> unidades) {}

    public record Dividir(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidadeNovaUnidade,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record RevisaoUnidade(@NotNull @Positive Long unidadeId, @NotNull @Min(0) Long versao) {}

    public record Reagrupar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versaoDestino,
            @NotNull @Size(min = 1, max = 20) List<@NotNull @Valid RevisaoUnidade> origens,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Resumo(
            Long id,
            long versao,
            String codigo,
            Long pedidoId,
            Long clienteId,
            Long armazemId,
            Long notaId,
            int serieNota,
            long numeroNota,
            Long produtoId,
            String sku,
            String unidadeMedida,
            boolean controlaLote,
            String lote,
            LocalDate validade,
            Long embalagemId,
            String codigoDun,
            BigDecimal quantidadeProdutoPorDun,
            TipoUnidadeLogistica tipo,
            CondicaoMercadoria condicao,
            BigDecimal quantidade,
            Instant dataFifo,
            Instant chegadaReal,
            Instant criadaEm,
            boolean ativa,
            boolean disponivelParaSaida) {
        public static Resumo de(UnidadeLogistica u, boolean disponivel) {
            return new Resumo(
                    u.getId(),
                    u.getVersao(),
                    u.getCodigo(),
                    u.getPedido().getId(),
                    u.getPedido().getCliente().getId(),
                    u.getPedido().getArmazem().getId(),
                    u.getNota().getId(),
                    u.getNota().getSerie(),
                    u.getNota().getNumero(),
                    u.getProduto().getId(),
                    u.getProduto().getSku(),
                    u.getProduto().getUnidadeMedida(),
                    u.getProduto().getControlaLote(),
                    u.getLote(),
                    u.getValidade(),
                    u.getEmbalagem().getId(),
                    u.getEmbalagem().getCodigoDun(),
                    u.getEmbalagem().getQuantidadeProduto(),
                    u.getTipo(),
                    u.getCondicao(),
                    u.getQuantidade(),
                    u.getDataFifo(),
                    u.getChegadaReal(),
                    u.getCriadaEm(),
                    u.isAtiva(),
                    disponivel);
        }
    }

    public record Origem(
            Long entradaId, Long itemChegadaId, Long itemNotaId, BigDecimal quantidadeAtual) {}

    public record Detalhe(Resumo unidade, List<Origem> origens) {}

    /** Resultado imutável da confirmação, também devolvido ao repetir a mesma operação. */
    public record Resultado(
            UUID operacaoId, Long pedidoId, long versaoPedido, List<Detalhe> unidades) {}

    public record Progresso(
            Long pedidoId,
            long entradasConferidas,
            long entradasUnitizadas,
            long entradasPendentes,
            boolean concluida) {}

    /** Contrato dos dados da etiqueta. Não envia comandos à impressora. */
    public record Etiqueta(
            String codigoLeitura,
            long versaoConteudo,
            String sku,
            Long clienteId,
            Long armazemId,
            Long notaId,
            int serieNota,
            long numeroNota,
            Instant dataEntrada,
            Instant chegadaReal,
            boolean controlaLote,
            String lote,
            LocalDate validade,
            TipoUnidadeLogistica tipo,
            CondicaoMercadoria condicao,
            BigDecimal quantidadeProduto,
            String unidadeMedida,
            String codigoDun,
            BigDecimal quantidadeProdutoPorDun) {
        public static Etiqueta de(UnidadeLogistica u) {
            return new Etiqueta(
                    u.getCodigo(),
                    u.getRevisaoConteudo(),
                    u.getProduto().getSku(),
                    u.getPedido().getCliente().getId(),
                    u.getPedido().getArmazem().getId(),
                    u.getNota().getId(),
                    u.getNota().getSerie(),
                    u.getNota().getNumero(),
                    u.getDataFifo(),
                    u.getChegadaReal(),
                    u.getProduto().getControlaLote(),
                    u.getLote(),
                    u.getValidade(),
                    u.getTipo(),
                    u.getCondicao(),
                    u.getQuantidade(),
                    u.getProduto().getUnidadeMedida(),
                    u.getEmbalagem().getCodigoDun(),
                    u.getEmbalagem().getQuantidadeProduto());
        }
    }
}
