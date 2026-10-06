package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.StatusResponse;
import br.com.rodogarcia.wms.services.StatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    private final StatusService service;

    public StatusController(StatusService service) {
        this.service = service;
    }

    @GetMapping
    public StatusResponse consultar() {
        return service.consultar();
    }
}
