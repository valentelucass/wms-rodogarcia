package br.com.rodogarcia.wms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TarefaColetorDto {
    private TarefaColetorDto() {}

    public record Disponibilidade(boolean disponivel, String mensagem) {}

    public record Totais(long enderecamento, long separacao, long inventario) {}

    public record Unidade(EstoqueDto.Unidade estoque, long revisaoConteudo) {}

    public record Destinos(List<EstoqueDto.Destino> destinos, Long conjuntoId) {}

    public record Tarefa(
            Long id,
            long versao,
            String tipo,
            Long origemId,
            Long clienteId,
            String cliente,
            Long armazemId,
            String armazem,
            String referencia,
            String sku,
            String descricao,
            String codigoUnidade,
            String situacao,
            String impedimento,
            Instant criadaEm,
            String responsavel,
            Instant iniciadaEm,
            boolean minha) {}

    public record Detalhe(
            Tarefa tarefa,
            EstoqueDto.Unidade unidade,
            PedidoSaidaDto.Detalhe pedido,
            List<ExpedicaoDto.Separacao> separacoes,
            InventarioDto.Resumo inventario) {}

    public record Comando(
            @NotNull UUID operacaoId,
            @NotNull @Min(0) Long versao,
            @NotBlank @Size(min = 5, max = 500) String motivo) {}

    public record Enderecar(
            @NotNull UUID operacaoId,
            @NotNull UUID codigoLido,
            @NotNull @Valid EstoqueDto.Posicionar posicionar) {}

    public record Separar(
            @NotNull UUID operacaoId,
            @NotNull @Valid ExpedicaoDto.Leitura leitura,
            @NotNull @Valid ExpedicaoDto.Destinacao destinacao) {}
}
