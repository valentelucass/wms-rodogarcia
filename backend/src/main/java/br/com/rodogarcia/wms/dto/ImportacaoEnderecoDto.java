package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ImportacaoEnderecoDto {
    private ImportacaoEnderecoDto() {}

    public record Previa(
            @NotNull UUID operacaoId, @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Confirmar(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 64, max = 64) String arquivoHash,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Linha(
            int linha,
            @NotNull @Valid EnderecoDto.Criar endereco,
            TipoUnidadeLogistica tipoUnidadePermitido) {}

    public record Erro(int linha, String coluna, String codigo, String mensagem) {}

    public record Resultado(
            Long id,
            long versao,
            Long armazemId,
            String arquivoHash,
            int layoutVersao,
            String situacao,
            int quantidadeLinhas,
            List<Linha> linhas,
            List<Erro> erros,
            List<EnderecoDto.Resposta> enderecos,
            Instant criadaEm,
            Instant confirmadaEm) {}
}
