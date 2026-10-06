package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.ContagemDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.ContagemEstoqueService;
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
@RequestMapping("/api/v1/contagens")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ContagemController {
    private final ContagemEstoqueService service;

    public ContagemController(ContagemEstoqueService service) {
        this.service = service;
    }

    @PostMapping
    public ContagemDto.Resultado contar(@RequestBody @Valid ContagemDto.Contar dados) {
        return service.contar(dados);
    }

    @GetMapping("/{id}")
    public ContagemDto.Resultado consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping
    public PaginaResponse<ContagemDto.Resultado> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) java.util.UUID codigoUnidade,
            @RequestParam(required = false) Long enderecoId,
            @RequestParam(required = false) ContagemDto.FiltroSituacao situacao,
            @RequestParam(required = false) Boolean impedimento,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(
                clienteId,
                armazemId,
                produtoId,
                codigoUnidade,
                enderecoId,
                situacao,
                impedimento,
                pagina,
                tamanho);
    }

    @GetMapping("/{id}/revisoes")
    public PaginaResponse<ContagemDto.Resultado> revisoes(
            @PathVariable Long id,
            @RequestParam(required = false) ContagemDto.FiltroSituacao situacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.revisoes(id, situacao, pagina, tamanho);
    }

    @PostMapping("/{id}/aplicar")
    public ContagemDto.Resultado aplicar(
            @PathVariable Long id, @RequestBody @Valid ContagemDto.Aplicar dados) {
        return service.aplicar(id, dados);
    }
}
