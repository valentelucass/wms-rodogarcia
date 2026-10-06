package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.SituacaoContingencia;
import br.com.rodogarcia.wms.models.TipoContingencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ContingenciaDto {
    private ContingenciaDto() {}

    public enum FiltroSituacao {
        PENDENTE,
        CONCILIADA
    }

    public enum FiltroTipo {
        CHEGADA,
        RESERVA,
        SEPARACAO,
        RETIRADA,
        RETORNO,
        ENTRADA,
        AVARIA,
        FATO_SERVICO,
        CONTAGEM,
        AJUSTE,
        REMANEJAMENTO
    }

    public record Registrar(
            @NotNull UUID operacaoId,
            @NotBlank @Size(max = 200) String identidadeFato,
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull TipoContingencia tipo,
            @NotNull Instant ocorridaEm,
            @NotBlank @Size(max = 200) String operador,
            @NotBlank @Size(min = 5, max = 500) String fonte,
            @NotNull Boolean efeitoRegistradoNoWms,
            @NotNull @Size(max = 200) List<@NotBlank @Size(max = 200) String> dependencias,
            @NotNull @Size(min = 1, max = 100) Map<String, Object> dados,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Conteudo(
            Map<String, Object> dados, List<String> dependencias, boolean efeitoRegistradoNoWms) {}

    public record Prova(
            @NotNull UUID operacaoOriginal,
            @NotBlank @Size(min = 64, max = 64) String conteudoHash) {}

    public record Conciliar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotNull @Pattern(regexp = "EXECUTAR|VINCULAR") String modo,
            @Valid Prova prova,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Resultado(
            Long id,
            long versao,
            String identidadeFato,
            Long clienteId,
            Long armazemId,
            TipoContingencia tipo,
            Instant ocorridaEm,
            String conteudoHash,
            Conteudo conteudo,
            SituacaoContingencia situacao,
            String pendencia,
            Instant conciliadaEm,
            Object resultado) {}
}
