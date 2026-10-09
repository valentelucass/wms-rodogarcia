package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class VisaoOperacaoDto {
    private VisaoOperacaoDto() {}

    public record Resumo(
            Long clienteId,
            Long armazemId,
            @NotNull Instant consultadoEm,
            @NotNull String fuso,
            Long capacidade,
            Long posicoesOcupadas,
            Long posicoesLivres,
            BigDecimal ocupacao,
            long unidadesArmazenadas,
            BigDecimal valorArmazenado,
            boolean valorCompleto,
            long emQuarentena,
            long reservasAtivas,
            long entradasAbertas,
            long saidasAbertas,
            BigDecimal faturamentoMes,
            boolean faturamentoParcial,
            boolean financeiroPermitido,
            @NotNull String competencia,
            @NotNull PaginaResponse<Posicao> mapa) {}

    public record Posicao(
            @NotNull Long id,
            @NotNull Long armazemId,
            @NotNull String armazem,
            @NotNull String codigo,
            @NotNull String rua,
            int nivel,
            @NotNull String posicao,
            @NotNull TipoEndereco tipo,
            @NotNull SituacaoCadastro situacao,
            @NotNull String estado,
            boolean disponivel,
            boolean ocupada,
            boolean bloqueada,
            boolean reservada,
            boolean quarentena,
            BigDecimal capacidadePesoKg) {}

    public record Unidade(
            @NotNull Long id,
            @NotNull Long clienteId,
            @NotNull String codigo,
            @NotNull String produto,
            @NotNull String sku,
            String lote,
            @NotNull BigDecimal quantidade,
            @NotNull String unidadeMedida,
            @NotNull Long pedidoEntradaId,
            boolean bloqueada,
            boolean reservada,
            boolean quarentena,
            Instant ultimaMovimentacao) {}

    public record Detalhe(
            @NotNull Posicao endereco,
            @NotNull List<Unidade> unidades,
            long unidadesVisiveis,
            boolean conteudoRestrito,
            BigDecimal alturaMetros,
            BigDecimal larguraMetros,
            BigDecimal profundidadeMetros,
            Integer empilhamentoMaximo,
            TipoUnidadeLogistica tipoUnidadePermitido) {}
}
