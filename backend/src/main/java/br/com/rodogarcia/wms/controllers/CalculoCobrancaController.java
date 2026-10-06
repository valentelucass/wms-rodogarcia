package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.CalculoCobrancaService;
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
@RequestMapping("/api/v1/calculos-cobranca")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class CalculoCobrancaController {
    private final CalculoCobrancaService service;

    public CalculoCobrancaController(CalculoCobrancaService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<CalculoCobrancaDto.Resultado> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(clienteId, armazemId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public CalculoCobrancaDto.Resultado consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @PostMapping
    public CalculoCobrancaDto.Resultado calcular(
            @RequestBody @Valid CalculoCobrancaDto.Calcular d) {
        return service.calcular(d);
    }
}
