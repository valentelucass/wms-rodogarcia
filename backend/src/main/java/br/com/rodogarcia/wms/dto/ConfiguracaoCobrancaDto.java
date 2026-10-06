package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ConfiguracaoCobrancaDto {
    private ConfiguracaoCobrancaDto() {}

    public record CriarServico(
            @NotNull UUID operacaoId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String codigo,
            @NotBlank @Size(max = 160) String descricao,
            @NotNull @Pattern(regexp = "ENTRADA|SAIDA|ARMAZENAGEM|ADICIONAL") String tipo,
            @NotNull
                    @Pattern(
                            regexp =
                                    "UNIDADE_LOGISTICA|QUANTIDADE_PRODUTO|POSICAO_DIA|VEICULO|CONTEINER|PERCENTUAL")
                    String unidade,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Servico(
            Long id,
            long versao,
            String codigo,
            String descricao,
            String tipo,
            String unidade,
            String situacao) {
        public static Servico de(ServicoCobranca s) {
            return new Servico(
                    s.getId(),
                    s.getVersao(),
                    s.getCodigo(),
                    s.getDescricao(),
                    s.getTipo(),
                    s.getUnidade(),
                    s.getSituacao());
        }
    }

    public record Item(
            @NotNull @Positive Long servicoId,
            @NotNull @Size(max = 40) String categoria,
            @DecimalMin("0") @Digits(integer = 13, fraction = 6) BigDecimal preco,
            @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 6)
                    BigDecimal percentual) {}

    public record CriarTabela(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long armazemId,
            @Positive Long clienteId,
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._/-]*")
                    String codigo,
            @NotBlank @Size(max = 160) String descricao,
            @NotNull @Pattern(regexp = "PADRAO|ESPECIFICA") String tipo,
            @NotNull LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            @NotNull @Size(min = 1, max = 500) List<@NotNull @Valid Item> itens,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record ItemResposta(
            Long id, Long servicoId, String categoria, BigDecimal preco, BigDecimal percentual) {
        public static ItemResposta de(ItemTabelaCobranca i) {
            return new ItemResposta(
                    i.getId(),
                    i.getServico().getId(),
                    i.getCategoria(),
                    i.getPreco(),
                    i.getPercentual());
        }
    }

    public record Tabela(
            Long id,
            long versao,
            Long armazemId,
            Long clienteId,
            String codigo,
            String tipo,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            String situacao,
            List<ItemResposta> itens) {
        public static Tabela de(TabelaCobranca t, List<ItemTabelaCobranca> itens) {
            return new Tabela(
                    t.getId(),
                    t.getVersao(),
                    t.getArmazem().getId(),
                    t.getCliente() == null ? null : t.getCliente().getId(),
                    t.getCodigo(),
                    t.getTipo(),
                    t.getVigenciaInicio(),
                    t.getVigenciaFim(),
                    t.getSituacao(),
                    itens.stream().map(ItemResposta::de).toList());
        }
    }

    public record Vincular(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull @Positive Long tabelaId,
            @NotNull LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record Vinculo(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            Long tabelaId,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim) {
        public static Vinculo de(VinculoTabelaCliente v) {
            return new Vinculo(
                    v.getId(),
                    v.getVersao(),
                    v.getCliente().getId(),
                    v.getArmazem().getId(),
                    v.getTabela().getId(),
                    v.getVigenciaInicio(),
                    v.getVigenciaFim());
        }
    }

    public record Encerrar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull LocalDate vigenciaFim,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record ConfigurarContrato(
            @NotNull UUID operacaoId,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            @NotBlank @Size(max = 60) String fuso,
            @NotNull @Pattern(regexp = "BRL") String moeda,
            @NotNull @Pattern(regexp = "MES_DIA_FIXO|DIAS_CORRIDOS") String modalidadeCiclo,
            @Min(1) @Max(31) Integer diaCorte,
            @Min(1) @Max(366) Integer duracaoDias,
            @NotNull @Pattern(regexp = "NAO_INFORMADO|NAO_APLICAVEL|APLICAVEL") String minimoModo,
            @NotNull @Pattern(regexp = "NAO_INFORMADO|NAO_APLICAVEL|APLICAVEL") String grisModo,
            @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal minimoValor,
            @Pattern(regexp = "INTEGRAL|PROPORCIONAL_DIAS") String minimoProporcao,
            @NotNull @Size(max = 500) List<@NotNull @Positive Long> servicosMinimo,
            @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 6)
                    BigDecimal grisPercentual,
            @Pattern(regexp = "VALOR_ESTOQUE_PICO|VALOR_ESTOQUE_MEDIO") String grisBase,
            @Pattern(regexp = "DIARIA|POR_CICLO") String grisPeriodicidade,
            @Pattern(regexp = "INTEGRAL|PROPORCIONAL_DIAS") String grisProporcao,
            @NotBlank @Size(min = 5, max = 500) String motivo,
            @Valid
                    @com.fasterxml.jackson.annotation.JsonInclude(
                            com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
                    FechamentoCobrancaDto.Resolucao resolucao) {}

    public record Contrato(
            Long id,
            long versao,
            Long clienteId,
            Long armazemId,
            LocalDate vigenciaInicio,
            LocalDate vigenciaFim,
            String fuso,
            String moeda,
            String modalidadeCiclo,
            Integer diaCorte,
            Integer duracaoDias,
            String minimoModo,
            String grisModo,
            BigDecimal minimoValor,
            String minimoProporcao,
            List<Long> servicosMinimo,
            BigDecimal grisPercentual,
            String grisBase,
            String grisPeriodicidade,
            String grisProporcao) {
        public static Contrato de(ContratoCobranca c, List<Long> servicos) {
            return new Contrato(
                    c.getId(),
                    c.getVersao(),
                    c.getCliente().getId(),
                    c.getArmazem().getId(),
                    c.getVigenciaInicio(),
                    c.getVigenciaFim(),
                    c.getFuso(),
                    c.getMoeda(),
                    c.getModalidadeCiclo(),
                    c.getDiaCorte(),
                    c.getDuracaoDias(),
                    c.getMinimoModo(),
                    c.getGrisModo(),
                    c.getMinimoValor(),
                    c.getMinimoProporcao(),
                    servicos,
                    c.getGrisPercentual(),
                    c.getGrisBase(),
                    c.getGrisPeriodicidade(),
                    c.getGrisProporcao());
        }
    }
}
