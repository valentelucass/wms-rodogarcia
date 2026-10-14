package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
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

public final class EnderecoDto {
    private EnderecoDto() {}

    public record Criar(
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String codigo,
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*") String rua,
            @NotNull @Min(0) @Max(999) Integer nivel,
            @NotBlank @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String posicao,
            @NotBlank @Size(max = 160) String descricao,
            @NotNull TipoEndereco tipo,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 3)
                    BigDecimal capacidadePesoKg,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal alturaMetros,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal larguraMetros,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal profundidadeMetros,
            @Min(1) @Max(999) Integer empilhamentoMaximo,
            @NotNull @Min(0) Integer sequenciaColeta,
            TipoUnidadeLogistica tipoUnidadePermitido) {
        public Criar(
                Long armazemId,
                String codigo,
                String rua,
                Integer nivel,
                String posicao,
                String descricao,
                TipoEndereco tipo,
                BigDecimal capacidadePesoKg,
                BigDecimal alturaMetros,
                BigDecimal larguraMetros,
                BigDecimal profundidadeMetros,
                Integer empilhamentoMaximo,
                Integer sequenciaColeta) {
            this(
                    armazemId,
                    codigo,
                    rua,
                    nivel,
                    posicao,
                    descricao,
                    tipo,
                    capacidadePesoKg,
                    alturaMetros,
                    larguraMetros,
                    profundidadeMetros,
                    empilhamentoMaximo,
                    sequenciaColeta,
                    null);
        }
    }

    public record Alterar(
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(max = 160) String descricao,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*") String codigo,
            @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*") String rua,
            @Min(0) @Max(999) Integer nivel,
            @Size(max = 20) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*") String posicao,
            TipoEndereco tipo,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 16, fraction = 3)
                    BigDecimal capacidadePesoKg,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal alturaMetros,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal larguraMetros,
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3)
                    BigDecimal profundidadeMetros,
            @Min(1) @Max(999) Integer empilhamentoMaximo,
            @Min(0) Integer sequenciaColeta,
            TipoUnidadeLogistica tipoUnidadePermitido) {
        public Alterar(
                Long versao,
                String descricao,
                String motivo,
                String codigo,
                String rua,
                Integer nivel,
                String posicao,
                TipoEndereco tipo,
                BigDecimal capacidadePesoKg,
                BigDecimal alturaMetros,
                BigDecimal larguraMetros,
                BigDecimal profundidadeMetros,
                Integer empilhamentoMaximo,
                Integer sequenciaColeta) {
            this(
                    versao,
                    descricao,
                    motivo,
                    codigo,
                    rua,
                    nivel,
                    posicao,
                    tipo,
                    capacidadePesoKg,
                    alturaMetros,
                    larguraMetros,
                    profundidadeMetros,
                    empilhamentoMaximo,
                    sequenciaColeta,
                    null);
        }

        public Alterar(Long versao, String descricao, String motivo) {
            this(
                    versao, descricao, motivo, null, null, null, null, null, null, null, null, null,
                    null, null);
        }
    }

    public record Resposta(
            Long id,
            long versao,
            SituacaoCadastro situacao,
            Instant criadoEm,
            Instant alteradoEm,
            Long armazemId,
            String codigo,
            String rua,
            int nivel,
            String posicao,
            String descricao,
            TipoEndereco tipo,
            BigDecimal capacidadePesoKg,
            BigDecimal alturaMetros,
            BigDecimal larguraMetros,
            BigDecimal profundidadeMetros,
            Integer empilhamentoMaximo,
            int sequenciaColeta,
            TipoUnidadeLogistica tipoUnidadePermitido) {
        public static Resposta de(Endereco e) {
            return new Resposta(
                    e.getId(),
                    e.getVersao(),
                    e.getSituacao(),
                    e.getCriadoEm(),
                    e.getAlteradoEm(),
                    e.getArmazem().getId(),
                    e.getCodigo(),
                    e.getRua(),
                    e.getNivel(),
                    e.getPosicao(),
                    e.getDescricao(),
                    e.getTipo(),
                    e.getCapacidadePesoKg(),
                    e.getAlturaMetros(),
                    e.getLarguraMetros(),
                    e.getProfundidadeMetros(),
                    e.getEmpilhamentoMaximo(),
                    e.getSequenciaColeta(),
                    e.getTipoUnidadePermitido());
        }
    }
}
