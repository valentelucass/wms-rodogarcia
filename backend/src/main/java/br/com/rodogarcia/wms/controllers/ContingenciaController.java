package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.ContingenciaDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.ContingenciaService;
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
@RequestMapping("/api/v1/contingencias")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ContingenciaController {
    private final ContingenciaService service;

    public ContingenciaController(ContingenciaService service) {
        this.service = service;
    }

    @PostMapping
    public ContingenciaDto.Resultado registrar(@RequestBody @Valid ContingenciaDto.Registrar d) {
        return service.registrar(d);
    }

    @PostMapping("/{id}/conciliar")
    public ContingenciaDto.Resultado conciliar(
            @PathVariable Long id, @RequestBody @Valid ContingenciaDto.Conciliar d) {
        return service.conciliar(id, d);
    }

    @GetMapping("/{id}")
    public ContingenciaDto.Resultado consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping
    public PaginaResponse<ContingenciaDto.Resultado> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(required = false) String identidadeFato,
            @RequestParam(required = false) ContingenciaDto.FiltroTipo tipo,
            @RequestParam(required = false) ContingenciaDto.FiltroSituacao situacao,
            @RequestParam(required = false) String pendencia,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(
                clienteId, armazemId, identidadeFato, tipo, situacao, pendencia, pagina, tamanho);
    }
}
