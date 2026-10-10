package br.com.rodogarcia.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class NfeImportacaoDto {
    private NfeImportacaoDto() {}

    public record Associacao(
            @NotNull @Min(1) @Max(990) Integer numeroItem, @NotNull @Positive Long produtoId) {}

    public record Ler(
            @NotBlank @Size(max = 1000000) String xml,
            @Positive Long clienteId,
            @Positive Long armazemId,
            @NotNull @Size(max = 200) List<@NotNull @Valid Associacao> associacoes) {}

    public record Confirmar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String referencia,
            @NotBlank @Size(max = 1000000) String xml,
            @NotBlank @Pattern(regexp = "[a-f0-9]{64}") String revisaoPrevia,
            @NotNull @Size(max = 200) List<@NotNull @Valid Associacao> associacoes) {}

    public record Participante(String documento, String nome) {}

    public record Item(
            int numeroItem,
            String codigo,
            String descricao,
            String gtin,
            String ncm,
            String cfop,
            String unidadeComercial,
            BigDecimal quantidadeComercial,
            BigDecimal valorUnitario,
            BigDecimal valorProduto,
            String unidadeTributavel,
            BigDecimal quantidadeTributavel,
            String gtinTributavel,
            String informacoesAdicionais) {}

    public record Volume(
            BigDecimal quantidade, String especie, BigDecimal pesoLiquido, BigDecimal pesoBruto) {}

    public record Protocolo(
            String chave,
            String ambiente,
            String numero,
            String recebidoEm,
            String codigoSituacao,
            String motivo) {}

    public record Documento(
            String versao,
            String modelo,
            String chaveAcesso,
            int serie,
            long numero,
            String emitidaEm,
            String tipoOperacao,
            String ambiente,
            String naturezaOperacao,
            Participante emitente,
            Participante destinatario,
            BigDecimal valorTotal,
            List<Item> itens,
            List<Volume> volumes,
            Protocolo protocolo,
            String informacoesComplementares,
            String informacoesFisco) {}

    public record ItemAssociado(
            int numeroItem,
            Long produtoId,
            Long produtoVersao,
            String sku,
            String descricao,
            String unidadeEstoque,
            BigDecimal quantidadeEstoque,
            BigDecimal fatorConversao,
            String pendencia) {}

    public record Previa(
            Documento documento,
            String xmlHash,
            String revisaoPrevia,
            Long clienteId,
            Long armazemId,
            List<ItemAssociado> itens,
            List<String> pendencias,
            List<String> avisos,
            Long pedidoExistenteId,
            boolean podeConfirmar) {}

    public record Confirmacao(
            String tipoRecurso, UUID operacaoId, String xmlHash, PedidoEntradaDto.Resumo pedido) {}

    public record DocumentoSalvo(Documento documento, String xmlOriginal, String xmlHash) {}
}
