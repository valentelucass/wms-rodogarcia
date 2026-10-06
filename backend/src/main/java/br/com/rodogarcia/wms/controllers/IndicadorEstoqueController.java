package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.IndicadorEstoqueDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.IndicadorEstoqueService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class IndicadorEstoqueController {
    private final IndicadorEstoqueService service;

    public IndicadorEstoqueController(IndicadorEstoqueService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/indicadores-estoque")
    public PaginaResponse<IndicadorEstoqueDto.Resultado> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(required = false) Long produtoId,
            @RequestParam(defaultValue = "false") boolean valor,
            @RequestParam String fuso,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(clienteId, armazemId, produtoId, valor, fuso, pagina, tamanho);
    }

    @GetMapping("/api/v1/avisos-validade")
    public IndicadorEstoqueDto.Configuracao consultar(
            @RequestParam Long clienteId, @RequestParam Long armazemId) {
        return service.configuracao(clienteId, armazemId);
    }

    @PutMapping("/api/v1/avisos-validade")
    public IndicadorEstoqueDto.Configuracao configurar(
            @RequestBody @Valid IndicadorEstoqueDto.ConfigurarAviso d) {
        return service.configurar(d);
    }
}
