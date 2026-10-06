package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.CargaInicialDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.CargaInicialService;
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
@RequestMapping("/api/v1/cargas-iniciais")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class CargaInicialController {
    private final CargaInicialService service;

    public CargaInicialController(CargaInicialService service) {
        this.service = service;
    }

    @PostMapping
    public CargaInicialDto.Resultado criar(@RequestBody @Valid CargaInicialDto.Criar d) {
        return service.criar(d);
    }

    @GetMapping("/{id}")
    public CargaInicialDto.Resultado consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping
    public PaginaResponse<CargaInicialDto.Resultado> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) CargaInicialDto.FiltroSituacao situacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(
                clienteId, armazemId, produtoId, referencia, situacao, pagina, tamanho);
    }

    @GetMapping("/{id}/revisoes")
    public PaginaResponse<CargaInicialDto.Revisao> revisoes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.revisoes(id, pagina, tamanho);
    }

    @PostMapping("/{id}/revisoes")
    public CargaInicialDto.Resultado revisar(
            @PathVariable Long id, @RequestBody @Valid CargaInicialDto.Revisar d) {
        return service.revisar(id, d);
    }

    @PostMapping("/{id}/preparar")
    public CargaInicialDto.Resultado preparar(
            @PathVariable Long id, @RequestBody @Valid CargaInicialDto.Confirmar d) {
        return service.preparar(id, d);
    }

    @PostMapping("/{id}/confirmar")
    public CargaInicialDto.Resultado confirmar(
            @PathVariable Long id, @RequestBody @Valid CargaInicialDto.Confirmar d) {
        return service.confirmar(id, d);
    }

    @PostMapping("/{id}/cancelar")
    public CargaInicialDto.Resultado cancelar(
            @PathVariable Long id, @RequestBody @Valid CargaInicialDto.Cancelar d) {
        return service.cancelar(id, d);
    }

    @PostMapping("/{id}/resolver-cancelamento")
    public CargaInicialDto.Resultado resolverCancelamento(
            @PathVariable Long id, @RequestBody @Valid CargaInicialDto.ResolverCancelamento d) {
        return service.resolverCancelamento(id, d);
    }
}
