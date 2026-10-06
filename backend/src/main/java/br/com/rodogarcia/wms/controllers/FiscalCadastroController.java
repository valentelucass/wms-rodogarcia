package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.services.FiscalCadastroService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalCadastroController {
    private final FiscalCadastroService service;

    public FiscalCadastroController(FiscalCadastroService service) {
        this.service = service;
    }

    @GetMapping("/clientes/{id}/complemento-fiscal")
    public FiscalCadastroDto.Complemento cliente(@PathVariable Long id) {
        return service.cliente(id);
    }

    @GetMapping("/armazens/{id}/complemento-fiscal")
    public FiscalCadastroDto.Complemento armazem(@PathVariable Long id) {
        return service.armazem(id);
    }

    @PostMapping("/clientes/{id}/complemento-fiscal")
    public FiscalCadastroDto.Complemento complementarCliente(
            @PathVariable Long id, @Valid @RequestBody FiscalCadastroDto.Complementar d) {
        return service.complementarCliente(id, d);
    }

    @PostMapping("/armazens/{id}/complemento-fiscal")
    public FiscalCadastroDto.Complemento complementarArmazem(
            @PathVariable Long id, @Valid @RequestBody FiscalCadastroDto.Complementar d) {
        return service.complementarArmazem(id, d);
    }

    @GetMapping("/produtos/{id}/referencias-fiscais")
    public List<FiscalCadastroDto.Referencia> referencias(@PathVariable Long id) {
        return service.referencias(id);
    }

    @PostMapping("/produtos/{id}/referencias-fiscais")
    public FiscalCadastroDto.Referencia referenciar(
            @PathVariable Long id, @Valid @RequestBody FiscalCadastroDto.Referenciar d) {
        return service.referenciar(id, d);
    }
}
