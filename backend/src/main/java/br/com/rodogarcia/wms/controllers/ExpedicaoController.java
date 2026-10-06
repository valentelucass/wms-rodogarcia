package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.services.ExpedicaoService;
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
@RequestMapping("/api/v1/pedidos-saida/{id}")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ExpedicaoController {
    private final ExpedicaoService service;

    public ExpedicaoController(ExpedicaoService service) {
        this.service = service;
    }

    @GetMapping("/expedicao")
    public ExpedicaoDto.Detalhe consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping("/fatos")
    public List<ExpedicaoDto.Fato> fatos(@PathVariable Long id) {
        return service.consultar(id).fatos();
    }

    @PostMapping("/leituras")
    public ExpedicaoDto.Confirmacao ler(
            @PathVariable Long id, @Valid @RequestBody ExpedicaoDto.Leitura d) {
        return service.ler(id, d);
    }

    @PostMapping("/separacoes")
    public ExpedicaoDto.Confirmacao separar(
            @PathVariable Long id, @Valid @RequestBody ExpedicaoDto.Separar d) {
        return service.separar(id, d);
    }

    @PostMapping("/documentos")
    public ExpedicaoDto.Confirmacao documento(
            @PathVariable Long id, @Valid @RequestBody ExpedicaoDto.Documento d) {
        return service.registrarDocumento(id, d);
    }

    @PostMapping("/documentos/{documentoId}/cancelamento")
    public ExpedicaoDto.Confirmacao cancelar(
            @PathVariable Long id,
            @PathVariable Long documentoId,
            @Valid @RequestBody ExpedicaoDto.CancelarDocumento d) {
        return service.cancelarDocumento(id, documentoId, d);
    }

    @PostMapping("/retirada")
    public ExpedicaoDto.Confirmacao retirar(
            @PathVariable Long id, @Valid @RequestBody ExpedicaoDto.Retirar d) {
        return service.retirar(id, d);
    }

    @PostMapping("/retorno-interno")
    public ExpedicaoDto.Confirmacao retornar(
            @PathVariable Long id, @Valid @RequestBody ExpedicaoDto.Retornar d) {
        return service.retornarInterno(id, d);
    }

    @PostMapping("/devolucoes")
    public ExpedicaoDto.Confirmacao devolver(
            @PathVariable Long id, @Valid @RequestBody ExpedicaoDto.Devolver d) {
        return service.devolver(id, d);
    }
}
