package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
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

public final class PedidoEntradaDto {
    private PedidoEntradaDto() {}

    public record Criar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String referencia) {}

    public record ItemNota(
            @NotNull @Min(1) @Max(990) Integer numeroItem,
            @NotNull @Positive Long produtoId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidadePrevista,
            @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal valorMercadoria) {}

    public record NotaManual(
            @NotNull @Min(0) Long versao,
            @NotNull @Min(0) @Max(999) Integer serie,
            @NotNull @Min(1) @Max(999999999) Long numero,
            @NotNull LocalDate emissao,
            @Pattern(regexp = "[0-9]{44}") String chaveAcesso,
            @NotNull @Size(min = 1, max = 200) List<@NotNull @Valid ItemNota> itens) {}

    public record ImportarXml(
            @NotNull @Min(0) Long versao, @NotBlank @Size(max = 1000000) String xml) {}

    public record Efetivar(
            @NotNull @Min(0) Long versao,
            @NotNull Boolean aceitarDivergencias,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Resumo(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            String referencia,
            SituacaoPedidoEntrada situacao,
            Instant criadoEm,
            Instant alteradoEm,
            Instant efetivadoEm,
            String motivoConclusao,
            String origemCriacao) {
        public static Resumo de(PedidoEntrada p) {
            return de(p, "MANUAL");
        }

        public static Resumo de(PedidoEntrada p, String origem) {
            return new Resumo(
                    p.getId(),
                    p.getVersao(),
                    p.getCliente().getId(),
                    p.getArmazem().getId(),
                    p.getReferencia(),
                    p.getSituacao(),
                    p.getCriadoEm(),
                    p.getAlteradoEm(),
                    p.getEfetivadoEm(),
                    p.getMotivoConclusao(),
                    origem);
        }
    }

    public record ItemConferencia(
            Long id,
            int numeroItem,
            Long produtoId,
            String sku,
            BigDecimal prevista,
            BigDecimal recebidaBoa,
            BigDecimal recebidaAvariada,
            BigDecimal diferenca,
            BigDecimal valorMercadoria) {}

    public record Nota(
            Long id,
            String emitente,
            int serie,
            long numero,
            LocalDate emissao,
            String chaveAcesso,
            boolean xmlVinculado,
            Instant primeiraChegada,
            List<ItemConferencia> itens) {}

    public record Detalhe(Resumo pedido, boolean divergente, List<Nota> notas) {}
}
