package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.TipoInventario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class InventarioDto {
    private InventarioDto() {}

    public record Disponibilidade(boolean disponivel, String mensagem) {}

    public record Iniciar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull TipoInventario tipo,
            @Positive Long produtoId,
            @Positive Long enderecoId,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Ler(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull UUID codigoUnidade,
            @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 6) BigDecimal contado,
            @Positive Long enderecoId,
            boolean substituir) {}

    public record Encerrar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            boolean confirmarNaoLidas,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Resumo(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            TipoInventario tipo,
            Long produtoId,
            Long enderecoId,
            String situacao,
            Instant iniciadoEm,
            Instant encerradoEm,
            String responsavel,
            long unidadesLidas,
            Integer divergencias) {}

    /** Somente o que o operador informou: nunca revela a base antes de encerrar. */
    public record Leitura(
            String codigoUnidade,
            String sku,
            String descricao,
            String unidadeMedida,
            BigDecimal contado,
            Long enderecoId,
            String posicao,
            Instant lidoEm) {}

    public record Detalhe(Resumo inventario, List<Leitura> leituras) {}

    public record Registro(Resumo inventario, Leitura leitura) {}

    public record Linha(
            String codigoUnidade,
            Long produtoId,
            String sku,
            String descricao,
            String unidadeMedida,
            BigDecimal esperado,
            BigDecimal contado,
            BigDecimal diferenca,
            String posicoesEsperadas,
            String posicaoEncontrada,
            List<String> ocorrencias) {}

    public record PorSku(
            Long produtoId,
            String sku,
            String descricao,
            String unidadeMedida,
            BigDecimal esperado,
            BigDecimal contado,
            BigDecimal diferenca,
            int mercadoriasDivergentes) {}

    public record Relatorio(
            Resumo inventario,
            Instant referenciaEstoque,
            int mercadoriasConferidas,
            int mercadoriasNaoLidas,
            int divergenciasQuantidade,
            int divergenciasEndereco,
            int movimentadasDuranteContagem,
            List<PorSku> porSku,
            List<Linha> mercadorias) {}

    public record Visao(
            Long produtoId,
            String sku,
            String descricao,
            String unidadeMedida,
            BigDecimal quantidade,
            long mercadorias,
            String posicoes) {}
}
