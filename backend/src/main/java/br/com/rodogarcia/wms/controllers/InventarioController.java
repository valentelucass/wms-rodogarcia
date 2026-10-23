package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.InventarioService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventarios")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class InventarioController {
    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    @GetMapping("/disponibilidade")
    public InventarioDto.Disponibilidade disponibilidade() {
        return service.disponibilidade();
    }

    @GetMapping("/visao")
    public PaginaResponse<InventarioDto.Visao> visao(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) Long enderecoId,
            @RequestParam(defaultValue = "") String pesquisa,
            @RequestParam(defaultValue = "SKU") String agrupar,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.visao(
                clienteId, armazemId, produtoId, enderecoId, pesquisa, agrupar, pagina, tamanho);
    }

    @GetMapping
    public PaginaResponse<InventarioDto.Resumo> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(clienteId, armazemId, pagina, tamanho);
    }

    @PostMapping
    public InventarioDto.Resumo iniciar(@RequestBody @Valid InventarioDto.Iniciar dados) {
        return service.iniciar(dados);
    }

    @GetMapping("/{id}")
    public InventarioDto.Detalhe consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @PostMapping("/{id}/leituras")
    public InventarioDto.Registro ler(
            @PathVariable Long id, @RequestBody @Valid InventarioDto.Ler dados) {
        return service.ler(id, dados);
    }

    @PostMapping("/{id}/finalizar")
    public InventarioDto.Relatorio finalizar(
            @PathVariable Long id, @RequestBody @Valid InventarioDto.Encerrar dados) {
        return service.finalizar(id, dados);
    }

    @PostMapping("/{id}/cancelar")
    public InventarioDto.Resumo cancelar(
            @PathVariable Long id, @RequestBody @Valid InventarioDto.Encerrar dados) {
        return service.cancelar(id, dados);
    }

    @GetMapping("/{id}/relatorio")
    public InventarioDto.Relatorio relatorio(@PathVariable Long id) {
        return service.relatorio(id);
    }
}
