package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.TarefaColetorDto;
import br.com.rodogarcia.wms.services.TarefaColetorService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tarefas-coletor")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class TarefaColetorController {
    private final TarefaColetorService service;

    public TarefaColetorController(TarefaColetorService service) {
        this.service = service;
    }

    @GetMapping("/disponibilidade")
    public TarefaColetorDto.Disponibilidade disponibilidade() {
        return service.disponibilidade();
    }

    @GetMapping("/resumo")
    public TarefaColetorDto.Totais totais(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long armazemId) {
        return service.totais(clienteId, armazemId);
    }

    @GetMapping
    public PaginaResponse<TarefaColetorDto.Tarefa> listar(
            @RequestParam String tipo,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(tipo, clienteId, armazemId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public TarefaColetorDto.Detalhe consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping("/pedido-saida/{pedidoId}")
    public TarefaColetorDto.Tarefa porPedido(@PathVariable Long pedidoId) {
        return service.porPedido(pedidoId);
    }

    @GetMapping("/{id}/destinos")
    public TarefaColetorDto.Destinos destinos(
            @PathVariable Long id,
            @RequestParam String codigo,
            @RequestParam(required = false) String segundoCodigo) {
        return service.destinos(id, codigo, segundoCodigo);
    }

    @GetMapping("/{id}/unidade")
    public TarefaColetorDto.Unidade unidade(@PathVariable Long id, @RequestParam UUID codigo) {
        return service.unidade(id, codigo);
    }

    @PostMapping("/{id}/iniciar")
    public TarefaColetorDto.Detalhe iniciar(
            @PathVariable Long id, @RequestBody @Valid TarefaColetorDto.Comando dados) {
        return service.iniciar(id, dados);
    }

    @PostMapping("/{id}/liberar")
    public TarefaColetorDto.Detalhe liberar(
            @PathVariable Long id, @RequestBody @Valid TarefaColetorDto.Comando dados) {
        return service.liberar(id, dados);
    }

    @PostMapping("/{id}/enderecar")
    public TarefaColetorDto.Detalhe enderecar(
            @PathVariable Long id, @RequestBody @Valid TarefaColetorDto.Enderecar dados) {
        return service.enderecar(id, dados);
    }

    @PostMapping("/{id}/separar")
    public TarefaColetorDto.Detalhe separar(
            @PathVariable Long id, @RequestBody @Valid TarefaColetorDto.Separar dados) {
        return service.separar(id, dados);
    }

    @PostMapping("/{id}/inventario/leituras")
    public InventarioDto.Registro contar(
            @PathVariable Long id, @RequestBody @Valid InventarioDto.Ler dados) {
        return service.contar(id, dados);
    }

    @PostMapping("/{id}/inventario/finalizar")
    public InventarioDto.Relatorio finalizar(
            @PathVariable Long id, @RequestBody @Valid InventarioDto.Encerrar dados) {
        return service.finalizar(id, dados);
    }

    @PostMapping("/{id}/inventario/cancelar")
    public InventarioDto.Resumo cancelarInventario(
            @PathVariable Long id, @RequestBody @Valid InventarioDto.Encerrar dados) {
        return service.cancelarInventario(id, dados);
    }
}
