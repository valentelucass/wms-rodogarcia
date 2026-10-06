package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.EstoqueService;
import br.com.rodogarcia.wms.services.MovimentacaoEstoqueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class EstoqueController {
    private final EstoqueService estoque;
    private final MovimentacaoEstoqueService movimentos;

    public EstoqueController(EstoqueService estoque, MovimentacaoEstoqueService movimentos) {
        this.estoque = estoque;
        this.movimentos = movimentos;
    }

    @GetMapping("/api/v1/estoque")
    public PaginaResponse<EstoqueDto.Unidade> listar(
            @RequestParam @Positive Long clienteId,
            @RequestParam @Positive Long armazemId,
            @RequestParam(required = false) @Positive Long produtoId,
            @RequestParam(required = false) Boolean disponivel,
            @RequestParam(required = false) UUID codigoUnidade,
            @RequestParam(required = false) @Positive Long enderecoId,
            @RequestParam(required = false) String situacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return estoque.listar(
                clienteId,
                armazemId,
                produtoId,
                codigoUnidade,
                enderecoId,
                disponivel,
                situacao,
                pagina,
                tamanho);
    }

    @GetMapping("/api/v1/estoque/saldo")
    public EstoqueDto.Saldo saldo(
            @RequestParam @Positive Long clienteId,
            @RequestParam @Positive Long armazemId,
            @RequestParam @Positive Long produtoId) {
        return estoque.saldo(clienteId, armazemId, produtoId);
    }

    @GetMapping("/api/v1/unidades-logisticas/{codigo}/estoque")
    public EstoqueDto.Unidade consultar(@PathVariable UUID codigo) {
        return estoque.consultar(codigo);
    }

    @GetMapping("/api/v1/unidades-logisticas/{codigo}/movimentos")
    public PaginaResponse<EstoqueDto.Movimento> historico(
            @PathVariable UUID codigo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return estoque.historico(codigo, pagina, tamanho);
    }

    @PostMapping("/api/v1/unidades-logisticas/{codigo}/movimentos")
    public EstoqueDto.Confirmacao posicionar(
            @PathVariable UUID codigo, @Valid @RequestBody EstoqueDto.Posicionar dados) {
        return movimentos.posicionar(codigo, dados);
    }

    @PostMapping("/api/v1/unidades-logisticas/{codigo}/bloqueio")
    public EstoqueDto.Confirmacao bloquear(
            @PathVariable UUID codigo, @Valid @RequestBody EstoqueDto.Bloqueio dados) {
        return movimentos.bloquear(codigo, dados);
    }

    @PostMapping("/api/v1/unidades-logisticas/{codigo}/liberacao")
    public EstoqueDto.Confirmacao liberar(
            @PathVariable UUID codigo, @Valid @RequestBody EstoqueDto.Bloqueio dados) {
        return movimentos.liberar(codigo, dados);
    }

    @PostMapping("/api/v1/unidades-logisticas/{codigo}/avaria")
    public EstoqueDto.Confirmacao avariar(
            @PathVariable UUID codigo, @Valid @RequestBody EstoqueDto.Bloqueio dados) {
        return movimentos.avariar(codigo, dados);
    }
}
