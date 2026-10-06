package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Ajustar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Ajuste;
import br.com.rodogarcia.wms.services.FechamentoCobrancaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ajustes-fechamento")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AjusteFechamentoController {
    private final FechamentoCobrancaService service;

    public AjusteFechamentoController(FechamentoCobrancaService service) {
        this.service = service;
    }

    @PostMapping
    public Ajuste ajustar(@RequestBody @Valid Ajustar d) {
        return service.ajustar(d);
    }

    @GetMapping
    public List<Ajuste> listar(@RequestParam Long clienteId, @RequestParam Long armazemId) {
        return service.ajustes(clienteId, armazemId);
    }
}
