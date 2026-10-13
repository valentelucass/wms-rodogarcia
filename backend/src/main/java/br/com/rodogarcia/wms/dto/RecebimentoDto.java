package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.ChegadaRecebimento;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ItemChegada;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class RecebimentoDto {
    private RecebimentoDto() {}

    public record Bobinas(
            @NotNull @Min(0) @Max(100) Integer boas,
            @NotNull @Min(0) @Max(100) Integer avariadas) {}

    public record Item(
            @NotNull @Positive Long itemNotaId,
            @Size(max = 60) String lote,
            LocalDate validade,
            @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 6) BigDecimal quantidadeBoa,
            @NotNull @DecimalMin("0") @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidadeAvariada,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    Bobinas bobinas) {
        public Item(
                Long itemNotaId,
                String lote,
                LocalDate validade,
                BigDecimal quantidadeBoa,
                BigDecimal quantidadeAvariada) {
            this(itemNotaId, lote, validade, quantidadeBoa, quantidadeAvariada, null);
        }
    }

    public record RegistrarChegada(
            @NotNull @Min(0) Long versao,
            @NotNull UUID operacaoId,
            @NotNull Instant chegouEm,
            @NotBlank @Size(min = 5, max = 500) String observacao,
            @NotNull @Size(min = 1, max = 200) List<@NotNull @Valid Item> itens) {}

    public record ItemFisico(
            Long id,
            Long itemNotaId,
            String lote,
            LocalDate validade,
            BigDecimal quantidadeBoa,
            BigDecimal quantidadeAvariada,
            @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    Bobinas bobinas) {
        public static ItemFisico de(ItemChegada i) {
            return de(i, null);
        }

        public static ItemFisico de(ItemChegada i, Bobinas bobinas) {
            return new ItemFisico(
                    i.getId(),
                    i.getItemNota().getId(),
                    i.getLote(),
                    i.getValidade(),
                    i.getQuantidadeBoa(),
                    i.getQuantidadeAvariada(),
                    bobinas);
        }
    }

    public record Chegada(
            Long id,
            String operacaoId,
            Instant chegouEm,
            Instant registradaEm,
            String usuario,
            String observacao,
            Instant estornadaEm,
            String estornadaPor,
            String motivoEstorno,
            List<ItemFisico> itens) {
        public static Chegada de(ChegadaRecebimento c, List<ItemFisico> itens) {
            return new Chegada(
                    c.getId(),
                    c.getOperacaoId(),
                    c.getChegouEm(),
                    c.getRegistradaEm(),
                    c.getUsuario(),
                    c.getObservacao(),
                    c.getEstornadaEm(),
                    c.getEstornadaPor(),
                    c.getMotivoEstorno(),
                    itens);
        }
    }

    public record Entrada(
            Long id,
            Long itemChegadaId,
            Long notaId,
            Long itemNotaId,
            Long produtoId,
            String lote,
            LocalDate validade,
            Instant chegadaReal,
            Instant dataFifo,
            Instant efetivadaEm,
            BigDecimal quantidadeTriagem,
            BigDecimal quantidadeQuarentena,
            Instant unitizadaEm,
            boolean disponivelParaSaida,
            @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    Bobinas bobinas) {
        public static Entrada de(EntradaConferida e) {
            return de(e, null);
        }

        public static Entrada de(EntradaConferida e, Bobinas bobinas) {
            var i = e.getItemChegada();
            return new Entrada(
                    e.getId(),
                    i.getId(),
                    i.getItemNota().getNota().getId(),
                    i.getItemNota().getId(),
                    i.getItemNota().getProduto().getId(),
                    i.getLote(),
                    i.getValidade(),
                    i.getChegada().getChegouEm(),
                    e.getDataFifo(),
                    e.getEfetivadaEm(),
                    e.getQuantidadeTriagem(),
                    e.getQuantidadeQuarentena(),
                    e.getUnitizadaEm(),
                    false,
                    bobinas);
        }
    }
}
