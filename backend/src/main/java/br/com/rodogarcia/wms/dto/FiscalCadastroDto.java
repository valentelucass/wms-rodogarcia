package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.ComplementoFiscal;
import br.com.rodogarcia.wms.models.ReferenciaFiscalProduto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class FiscalCadastroDto {
    private FiscalCadastroDto() {}

    public record Dados(
            @Size(max = 160) String razaoSocial,
            @Size(max = 30) String inscricaoEstadual,
            @Size(max = 160) String logradouro,
            @Size(max = 20) String numeroEndereco,
            @Size(max = 80) String complemento,
            @Size(max = 80) String bairro,
            @Size(max = 100) String cidade,
            @Pattern(regexp = "[A-Z]{2}") String uf,
            @Size(max = 16) String cep,
            @Pattern(regexp = "[A-Z]{2}") String pais,
            @Size(max = 120) String contatoNome,
            @Email @Size(max = 160) String contatoEmail,
            @Size(max = 30) String contatoTelefone,
            @Email @Size(max = 160) String faturamentoEmail,
            @Size(max = 500) String faturamentoReferencia) {
        public ComplementoFiscal modelo() {
            return new ComplementoFiscal(
                    razaoSocial,
                    inscricaoEstadual,
                    logradouro,
                    numeroEndereco,
                    complemento,
                    bairro,
                    cep,
                    pais,
                    contatoNome,
                    contatoEmail,
                    contatoTelefone);
        }

        public static Dados de(
                ComplementoFiscal c, String cidade, String uf, String email, String referencia) {
            return c == null
                    ? new Dados(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            cidade,
                            uf,
                            null,
                            null,
                            null,
                            null,
                            null,
                            email,
                            referencia)
                    : new Dados(
                            c.getRazaoSocial(),
                            c.getInscricaoEstadual(),
                            c.getLogradouro(),
                            c.getNumeroEndereco(),
                            c.getComplemento(),
                            c.getBairro(),
                            cidade,
                            uf,
                            c.getCep(),
                            c.getPais(),
                            c.getContatoNome(),
                            c.getContatoEmail(),
                            c.getContatoTelefone(),
                            email,
                            referencia);
        }
    }

    public record Complementar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Valid Dados dados,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Complemento(
            Long id, long versao, String codigo, String documentoFiscal, Dados dados) {}

    public record Referenciar(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long armazemId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String operacao,
            @NotNull @Min(0) Long versao,
            @Pattern(regexp = "[0-9]{8}") String ncm,
            @Pattern(regexp = "[0-9]{4}") String cfop,
            @Pattern(regexp = "[0-9]{7}") String cest,
            @Size(max = 500) String enquadramento,
            @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 6)
                    BigDecimal aliquotaIcms,
            @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 6)
                    BigDecimal aliquotaIpi,
            @NotBlank @Size(max = 200) String fonte,
            @Size(max = 200) String conferidaPor,
            Instant conferidaEm,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Referencia(
            Long id,
            long versao,
            Long produtoId,
            Long armazemId,
            String operacao,
            String ncm,
            String cfop,
            String cest,
            String enquadramento,
            BigDecimal aliquotaIcms,
            BigDecimal aliquotaIpi,
            String fonte,
            String conferidaPor,
            Instant conferidaEm) {
        public static Referencia de(ReferenciaFiscalProduto r) {
            return new Referencia(
                    r.getId(),
                    r.getVersao(),
                    r.getProduto().getId(),
                    r.getArmazem().getId(),
                    r.getOperacao(),
                    r.getNcm(),
                    r.getCfop(),
                    r.getCest(),
                    r.getEnquadramento(),
                    r.getAliquotaIcms(),
                    r.getAliquotaIpi(),
                    r.getFonte(),
                    r.getConferidaPor(),
                    r.getConferidaEm());
        }
    }
}
