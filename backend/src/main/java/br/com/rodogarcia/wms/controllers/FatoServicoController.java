package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.FatoServicoService;
import br.com.rodogarcia.wms.services.MarcoFinanceiroAvariaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FatoServicoController {
    private final FatoServicoService service;
    private final MarcoFinanceiroAvariaService marcos;

    public FatoServicoController(FatoServicoService service, MarcoFinanceiroAvariaService marcos) {
        this.service = service;
        this.marcos = marcos;
    }

    @GetMapping("/fatos-servico")
    public PaginaResponse<FatoServicoDto.Fato> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(clienteId, armazemId, pagina, tamanho);
    }

    @GetMapping("/fatos-servico/{id}")
    public FatoServicoDto.Fato consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping("/fatos-servico/sugestoes")
    public PaginaResponse<FatoServicoDto.Sugestao> sugestoes(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam Long servicoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.sugestoes(clienteId, armazemId, servicoId, pagina, tamanho);
    }

    @PostMapping("/fatos-servico")
    public FatoServicoDto.Fato registrar(@RequestBody @Valid FatoServicoDto.Registrar d) {
        return service.registrar(d);
    }

    @PostMapping("/fatos-servico/{id}/anulacao")
    public FatoServicoDto.Fato anular(
            @PathVariable Long id, @RequestBody @Valid FatoServicoDto.Anular d) {
        return service.anular(id, d);
    }

    @GetMapping("/avarias/{id}/marcos-financeiros")
    public List<FatoServicoDto.MarcoResposta> marcos(@PathVariable Long id) {
        return marcos.listar(id);
    }

    @PostMapping("/avarias/{id}/marcos-financeiros")
    public FatoServicoDto.MarcoResposta marco(
            @PathVariable Long id, @RequestBody @Valid FatoServicoDto.Marco d) {
        return marcos.registrar(id, d);
    }
}
