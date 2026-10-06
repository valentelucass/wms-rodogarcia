package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
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

public final class CapacidadeDto {
    private CapacidadeDto() {}

    public record Limites(
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 3)
                    BigDecimal pesoKg,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal alturaMetros,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal larguraMetros,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal profundidadeMetros,
            @NotNull @Min(1) @Max(999) Integer empilhamentoMaximo) {}

    public record ConfigurarEndereco(
            @NotNull @Min(0) Long versao,
            @NotNull TipoUnidadeLogistica tipoUnidadePermitido,
            @NotNull @Valid Limites limites,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record CriarConjunto(
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String codigo,
            @NotNull @Positive Long enderecoAId,
            @NotNull @Positive Long enderecoBId,
            @NotNull @Valid Limites limites,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Conjunto(
            Long id,
            long versao,
            SituacaoCadastro situacao,
            Long armazemId,
            String codigo,
            Long enderecoAId,
            Long enderecoBId,
            TipoEndereco tipoEndereco,
            TipoUnidadeLogistica tipoUnidadePermitido,
            Limites limites) {
        public static Conjunto de(ConjuntoPosicoes c) {
            return new Conjunto(
                    c.getId(),
                    c.getVersao(),
                    c.getSituacao(),
                    c.getArmazem().getId(),
                    c.getCodigo(),
                    c.getEnderecoA().getId(),
                    c.getEnderecoB().getId(),
                    c.getEnderecoA().getTipo(),
                    c.getEnderecoA().getTipoUnidadePermitido(),
                    new Limites(
                            c.getCapacidadePesoKg(),
                            c.getAlturaMetros(),
                            c.getLarguraMetros(),
                            c.getProfundidadeMetros(),
                            c.getEmpilhamentoMaximo()));
        }
    }
}
