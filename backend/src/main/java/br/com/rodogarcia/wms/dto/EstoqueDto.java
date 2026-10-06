package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.MedidasUnidade;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EstoqueDto {
    private EstoqueDto() {}

    public record Medidas(
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 3)
                    BigDecimal pesoKg,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal alturaMetros,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal larguraMetros,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal profundidadeMetros,
            @NotNull @Min(1) @Max(999) Integer empilhamento,
            @NotNull @Min(1) @Max(2) Integer posicoesNecessarias) {
        public static Medidas de(MedidasUnidade m) {
            return m == null
                    ? null
                    : new Medidas(
                            m.getPesoKg(),
                            m.getAlturaMetros(),
                            m.getLarguraMetros(),
                            m.getProfundidadeMetros(),
                            m.getEmpilhamento(),
                            m.getPosicoesNecessarias());
        }
    }

    public record Destino(
            @NotNull @Positive Long enderecoId, @NotBlank @Size(max = 40) String codigoLido) {}

    public record Posicionar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versaoUnidade,
            @Valid Medidas medidas,
            @Positive Long conjuntoId,
            @NotNull @Size(min = 1, max = 2) List<@NotNull @Valid Destino> destinos,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Bloqueio(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versaoUnidade,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Posicao(
            Long enderecoId,
            String codigo,
            TipoEndereco tipo,
            SituacaoCadastro situacao,
            CapacidadeDto.Limites limites) {}

    public record Unidade(
            UnidadeLogisticaDto.Resumo unidade,
            Medidas medidas,
            TipoEndereco tipoLocalizacao,
            boolean bloqueada,
            boolean avariaPosterior,
            boolean avariaInicialReparada,
            Instant primeiroEnderecamentoEm,
            Instant inicioArmazenagemEm,
            int posicoesEquivalentes,
            Long conjuntoId,
            List<Posicao> posicoes) {}

    public record Confirmacao(
            UUID operacaoId,
            Long pedidoId,
            long versaoPedido,
            Unidade estoque,
            AvariaDto.Ocorrencia avaria) {
        public Confirmacao(UUID operacaoId, Long pedidoId, long versaoPedido, Unidade estoque) {
            this(operacaoId, pedidoId, versaoPedido, estoque, null);
        }
    }

    public record Movimento(
            Long id,
            String operacaoId,
            String acao,
            String usuario,
            String motivo,
            Instant instante,
            Unidade antes,
            Confirmacao depois) {}

    public record Saldo(
            Long clienteId,
            Long armazemId,
            Long produtoId,
            String unidadeMedida,
            BigDecimal fisicoTotal,
            BigDecimal pendenteUnitizacao,
            BigDecimal fisicoUnitizado,
            BigDecimal disponivel,
            BigDecimal reservado,
            BigDecimal bloqueado,
            BigDecimal naoEnderecado,
            BigDecimal emTriagem,
            BigDecimal emQuarentena,
            BigDecimal emArmazenagem,
            BigDecimal avariado) {}
}
