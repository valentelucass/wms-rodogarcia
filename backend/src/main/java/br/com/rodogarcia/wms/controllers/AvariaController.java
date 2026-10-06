package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.AvariaDto;
import br.com.rodogarcia.wms.services.AvariaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/estoque/unidades/{codigo}/avarias")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AvariaController {
    private final AvariaService service;

    public AvariaController(AvariaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AvariaDto.Ocorrencia> listar(@PathVariable UUID codigo) {
        return service.listar(codigo);
    }

    @PostMapping
    public AvariaDto.Confirmacao registrar(
            @PathVariable UUID codigo, @Valid @RequestBody AvariaDto.Registrar d) {
        return service.registrar(codigo, d);
    }

    @PostMapping("/{id}/responsabilidade")
    public AvariaDto.Confirmacao reconhecer(
            @PathVariable UUID codigo,
            @PathVariable Long id,
            @Valid @RequestBody AvariaDto.Reconhecer d) {
        return service.reconhecer(codigo, id, d);
    }

    @PostMapping("/{id}/reparo")
    public AvariaDto.Confirmacao reparar(
            @PathVariable UUID codigo,
            @PathVariable Long id,
            @Valid @RequestBody AvariaDto.Reparar d) {
        return service.reparar(codigo, id, d);
    }
}
