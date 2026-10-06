package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ArmazemDto {
    private ArmazemDto() {}

    public record Criar(
            @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String codigo,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(max = 30) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9./ -]*")
                    String documentoFiscal,
            @NotBlank @Size(max = 100) String cidade,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String uf) {}

    public record Alterar(
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(max = 120) String nome,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Resposta(
            Long id,
            long versao,
            SituacaoCadastro situacao,
            Instant criadoEm,
            Instant alteradoEm,
            String codigo,
            String nome,
            String documentoFiscal,
            String cidade,
            String uf) {
        public static Resposta de(Armazem e) {
            return new Resposta(
                    e.getId(),
                    e.getVersao(),
                    e.getSituacao(),
                    e.getCriadoEm(),
                    e.getAlteradoEm(),
                    e.getCodigo(),
                    e.getNome(),
                    e.getDocumentoFiscal(),
                    e.getCidade(),
                    e.getUf());
        }
    }
}
