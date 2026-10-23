package br.com.rodogarcia.wms.dto;

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

public final class FiscalNfeDto {
    private FiscalNfeDto() {}

    public record Preparar(
            @NotNull @Min(0) Long versao,
            @NotBlank @Pattern(regexp = "RETORNO_MERCADORIA|RETORNO_SIMBOLICO|REGULARIZACAO_ENTRADA") String natureza,
            @Positive Long documentoOrigemId) {}

    public record Confirmar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String hash,
            @NotBlank @Pattern(regexp = "RETORNO_MERCADORIA|RETORNO_SIMBOLICO|REGULARIZACAO_ENTRADA") String natureza,
            @Positive Long documentoOrigemId,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Comando(@NotNull UUID operacaoId, @NotBlank @Size(min = 15, max = 500) String motivo) {}

    public record Item(
            @NotNull Long reservaId, @NotNull Long entradaOrigemId, @NotNull Long itemNotaOrigemId, @NotNull Long notaOrigemId, @NotNull Long produtoId,
            @NotNull String sku, @NotNull String descricao, @NotNull String unidade, @NotNull BigDecimal quantidade,
            @NotNull BigDecimal valorUnitario, @NotNull BigDecimal valorTotal, @NotNull String ncm, @NotNull String cfop,
            @NotNull String chaveOrigem, long revisaoConteudo) {}

    public record DocumentoPlanejado(
            @NotNull String grupo, @NotNull String natureza, @NotNull String emitenteCnpj, @NotNull String destinatarioDocumento,
            String destinatarioNome, String naturezaOperacao, String finalidade, int ambiente,
            Long documentoOrigemId, @NotNull List<Item> itens, @NotNull List<ExpedicaoDto.Cobertura> coberturas,
            @NotNull List<String> chavesReferenciadas, @NotNull BigDecimal valorTotal, @NotNull String observacao) {}

    public record Previa(
            @NotNull Long pedidoId, long versao, @NotNull String hash, boolean podeEmitir,
            boolean integracaoHabilitada, @NotNull List<String> pendencias,
            @NotNull List<DocumentoPlanejado> documentos, @NotNull ExpedicaoDto.Detalhe expedicao) {}

    public record Solicitacao(
            @NotNull String id, @NotNull String planoId, @NotNull String natureza, @NotNull String situacao,
            String numero, String serie, String chaveAcesso, String protocolo,
            Long documentoId, Long documentoOrigemId, @NotNull String mensagem,
            boolean podeProcessar, boolean podeCancelar, boolean xmlDisponivel,
            @NotNull Instant criadaEm, @NotNull Instant atualizadaEm) {}

    public record Andamento(@NotNull Long pedidoId, @NotNull String situacao, @NotNull List<Solicitacao> solicitacoes) {}

    public record Linha(
            @NotNull Long id, long versao, @NotNull String referencia, @NotNull Instant criadoEm, @NotNull Long clienteId,
            @NotNull String cliente, @NotNull Long armazemId, @NotNull String situacaoOperacional, @NotNull String situacaoFiscal,
            @NotNull List<String> numerosNfe, boolean podeEmitir) {}

    public record Arquivo(@NotNull String nome, @NotNull String conteudo) {}
    public record RetornoOperacional(@NotNull Long pedidoId, long versao, boolean houveRetirada,
            @NotNull List<String> regularizacoesFiscais, @NotNull String solicitacaoEntradaId,
            @NotNull ExpedicaoDto.Nota nota, @NotNull List<ExpedicaoDto.ItemDevolucao> itens) {}
}
