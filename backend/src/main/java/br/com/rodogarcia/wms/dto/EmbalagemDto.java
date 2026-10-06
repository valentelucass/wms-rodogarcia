package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
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

public final class EmbalagemDto {
    private EmbalagemDto() {}

    public record Criar(
            @NotNull @Positive Long produtoId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String codigoDun,
            @NotBlank @Size(max = 160) String descricao,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 13, fraction = 6)
                    BigDecimal quantidadeProduto) {}

    public record Alterar(
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(max = 160) String descricao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Resposta(
            Long id,
            long versao,
            SituacaoCadastro situacao,
            Instant criadoEm,
            Instant alteradoEm,
            Long produtoId,
            String codigoDun,
            String descricao,
            BigDecimal quantidadeProduto) {
        public static Resposta de(Embalagem e) {
            return new Resposta(
                    e.getId(),
                    e.getVersao(),
                    e.getSituacao(),
                    e.getCriadoEm(),
                    e.getAlteradoEm(),
                    e.getProduto().getId(),
                    e.getCodigoDun(),
                    e.getDescricao(),
                    e.getQuantidadeProduto());
        }
    }
}
