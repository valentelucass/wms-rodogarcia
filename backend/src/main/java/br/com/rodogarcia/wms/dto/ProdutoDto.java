package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ProdutoDto {
    private ProdutoDto() {}

    public record Criar(
            @NotNull @Positive Long clienteId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*") String sku,
            @NotBlank @Size(max = 160) String descricao,
            @NotBlank @Pattern(regexp = "[A-Za-z]{1,8}") String unidadeMedida,
            @NotNull TipoQuantidade tipoQuantidade,
            @NotNull @Min(0) @Max(6) Integer precisaoQuantidade,
            @NotNull Boolean controlaLote,
            @NotNull Boolean controlaValidade,
            @Min(0) @Max(3650) Integer antecedenciaAvisoDias) {}

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
            Long clienteId,
            String sku,
            String descricao,
            String unidadeMedida,
            TipoQuantidade tipoQuantidade,
            int precisaoQuantidade,
            boolean controlaLote,
            boolean controlaValidade,
            Integer antecedenciaAvisoDias) {
        public static Resposta de(Produto e) {
            return new Resposta(
                    e.getId(),
                    e.getVersao(),
                    e.getSituacao(),
                    e.getCriadoEm(),
                    e.getAlteradoEm(),
                    e.getCliente().getId(),
                    e.getSku(),
                    e.getDescricao(),
                    e.getUnidadeMedida(),
                    e.getTipoQuantidade(),
                    e.getPrecisaoQuantidade(),
                    e.getControlaLote(),
                    e.getControlaValidade(),
                    e.getAntecedenciaAvisoDias());
        }
    }
}
