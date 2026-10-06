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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ExpedicaoDto {
    private ExpedicaoDto() {}

    public record Leitura(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Positive Long reservaId,
            @NotNull UUID codigoLido,
            @NotNull @Min(0) Long revisaoConteudo,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Leitura {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record Destinacao(
            @NotNull @Positive Long reservaId,
            @Positive Long conjuntoId,
            @NotNull @Size(min = 1, max = 2) List<@NotNull @Valid Destino> destinos) {}

    public record Separar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Valid Destinacao destinacao,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Separar {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record Nota(
            @NotBlank @Pattern(regexp = "[0-9A-Z]{14}") String emitenteCnpj,
            @NotBlank @Pattern(regexp = "[0-9]{1,3}") String serie,
            @NotBlank @Pattern(regexp = "[0-9]{1,9}") String numero,
            @NotNull LocalDate emissao,
            @Pattern(regexp = "[0-9]{44}") String chaveAcesso) {}

    public record Cobertura(
            @NotNull @Positive Long reservaId,
            @NotNull @Positive Long notaOrigemId,
            @NotBlank @Size(max = 40) String sku,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade) {}

    public record Documento(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Pattern(regexp = "NOTAZZ|XML") String origem,
            @NotBlank @Pattern(regexp = "RETORNO_MERCADORIA|RETORNO_SIMBOLICO") String natureza,
            @Valid Nota nota,
            @NotBlank @Size(max = 60) String protocolo,
            @Size(max = 1048576) String xml,
            @NotNull @Size(min = 1) List<@NotNull @Valid Cobertura> coberturas,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Documento {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record CancelarDocumento(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Retirar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Size(min = 1) List<@NotBlank @Size(max = 1048576) String> xmls,
            @NotNull List<@NotNull @Valid Destinacao> remanescentes,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Retirar {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record Retornar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull List<@NotNull @Valid Destinacao> unidades,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Retornar {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record ItemDevolucao(
            @NotNull @Positive Long baixaId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidade,
            @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidadeAvariada) {}

    public record Devolver(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(max = 40) String referencia,
            @NotNull @Valid Nota nota,
            @NotNull Instant chegadaReal,
            @NotNull @Size(min = 1) List<@NotNull @Valid ItemDevolucao> itens,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            Boolean resolverPendentes) {
        public Devolver {
            if (resolverPendentes == null) resolverPendentes = false;
        }
    }

    public record Separacao(
            Long id,
            Long reservaId,
            String situacao,
            long revisaoConteudoLida,
            Instant lidaEm,
            Instant separadaEm,
            Instant encerradaEm,
            List<EstoqueDto.Destino> posicoesOrigem,
            Long conjuntoOrigemId) {}

    public record DocumentoRegistrado(
            Long id,
            String origem,
            String natureza,
            Nota nota,
            String protocolo,
            String situacao,
            Instant registradoEm,
            Instant canceladoEm,
            List<Cobertura> coberturas) {}

    public record Baixa(
            Long id,
            Long reservaId,
            Long entradaOrigemId,
            BigDecimal quantidade,
            Instant dataFifo,
            Instant inicioArmazenagemEm,
            BigDecimal posicoesEquivalentes) {}

    public record Devolucao(
            Long id,
            Long baixaId,
            Long pedidoEntradaId,
            Long entradaNovaId,
            BigDecimal quantidade,
            Instant dataFifo,
            Instant registradaEm) {}

    public record Fato(
            Long id,
            Long unidadeId,
            String tipo,
            Instant ocorridaEm,
            Instant registradaEm,
            BigDecimal quantidadeAntes,
            BigDecimal quantidadeDepois,
            BigDecimal equivalenciaAntes,
            BigDecimal equivalenciaDepois) {}

    public record Detalhe(
            PedidoSaidaDto.Detalhe pedido,
            List<Separacao> separacoes,
            List<DocumentoRegistrado> documentos,
            Instant retiradaEm,
            List<Baixa> baixas,
            List<Devolucao> devolucoes,
            List<Fato> fatos) {}

    public record Confirmacao(UUID operacaoId, Detalhe expedicao, Long pedidoEntradaId) {}
}
