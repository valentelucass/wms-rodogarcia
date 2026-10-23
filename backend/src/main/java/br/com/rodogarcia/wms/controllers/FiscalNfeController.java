package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.FiscalNfeService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos-saida")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalNfeController {
    private final FiscalNfeService service;

    public FiscalNfeController(FiscalNfeService service) {
        this.service = service;
    }

    @GetMapping("/fiscal")
    public PaginaResponse<FiscalNfeDto.Linha> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(defaultValue = "TODOS") String situacao,
            @RequestParam(defaultValue = "") String busca,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "25") int tamanho) {
        return service.listar(clienteId, armazemId, situacao, busca, pagina, tamanho);
    }

    @PostMapping("/{id}/fiscal/previa")
    public FiscalNfeDto.Previa previa(
            @PathVariable Long id, @Valid @RequestBody FiscalNfeDto.Preparar d) {
        return service.previa(id, d);
    }

    @PostMapping("/{id}/fiscal/emissoes")
    public FiscalNfeDto.Andamento confirmar(
            @PathVariable Long id, @Valid @RequestBody FiscalNfeDto.Confirmar d) {
        return service.confirmar(id, d);
    }

    @GetMapping("/{id}/fiscal")
    public FiscalNfeDto.Andamento andamento(@PathVariable Long id) {
        return service.andamento(id);
    }

    @PostMapping("/{id}/fiscal/{solicitacaoId}/processamento")
    public FiscalNfeDto.Andamento processar(
            @PathVariable Long id, @PathVariable UUID solicitacaoId) {
        return service.processar(id, solicitacaoId);
    }

    @PostMapping("/{id}/fiscal/{solicitacaoId}/cancelamento")
    public FiscalNfeDto.Andamento cancelar(
            @PathVariable Long id,
            @PathVariable UUID solicitacaoId,
            @Valid @RequestBody FiscalNfeDto.Comando d) {
        return service.cancelar(id, solicitacaoId, d);
    }

    @GetMapping("/{id}/fiscal/{solicitacaoId}/xml")
    public FiscalNfeDto.Arquivo arquivo(@PathVariable Long id, @PathVariable UUID solicitacaoId) {
        return service.arquivo(id, solicitacaoId);
    }

    @GetMapping("/{id}/fiscal/{solicitacaoId}/retorno")
    public FiscalNfeDto.RetornoOperacional retorno(
            @PathVariable Long id, @PathVariable UUID solicitacaoId) {
        return service.retorno(id, solicitacaoId);
    }
}
