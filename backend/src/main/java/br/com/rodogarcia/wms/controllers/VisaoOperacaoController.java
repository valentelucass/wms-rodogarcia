package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.VisaoOperacaoDto;
import br.com.rodogarcia.wms.services.VisaoOperacaoService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class VisaoOperacaoController {
    private final VisaoOperacaoService service;

    public VisaoOperacaoController(VisaoOperacaoService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/visao-operacao")
    public VisaoOperacaoDto.Resumo consultar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) Long armazemId,
            @RequestParam String fuso,
            @RequestParam(defaultValue = "") String codigo,
            @RequestParam(defaultValue = "TODAS") String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "100") int tamanho) {
        return service.consultar(clienteId, armazemId, fuso, codigo, estado, pagina, tamanho);
    }

    @GetMapping("/api/v1/visao-operacao/posicoes/{id}")
    public VisaoOperacaoDto.Detalhe detalhe(@PathVariable Long id) {
        return service.detalhe(id);
    }
}
