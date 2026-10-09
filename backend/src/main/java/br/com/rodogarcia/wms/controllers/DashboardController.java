package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.DashboardDto;
import br.com.rodogarcia.wms.services.DashboardService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/dashboard")
    public DashboardDto.Resumo consultar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam String fuso,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "6") int tamanho) {
        return service.consultar(clienteId, armazemId, fuso, pagina, tamanho);
    }
}
