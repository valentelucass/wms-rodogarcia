package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.AuditoriaResponse;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.AuditoriaService;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditoria")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AuditoriaController {
    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<AuditoriaResponse> listar(
            @RequestParam String tipo,
            @RequestParam @Positive Long registroId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(tipo, registroId, pagina, tamanho);
    }
}
