package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.services.ClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clientes")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ClienteController {
    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<ClienteDto.Resposta> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(pagina, tamanho);
    }

    @GetMapping("/{id}")
    public ClienteDto.Resposta consultar(@PathVariable @Positive Long id) {
        return service.consultar(id);
    }

    @PostMapping
    public ResponseEntity<ClienteDto.Resposta> criar(@Valid @RequestBody ClienteDto.Criar dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + resposta.id()))
                .body(resposta);
    }

    @PutMapping("/{id}")
    public ClienteDto.Resposta alterar(
            @PathVariable @Positive Long id, @Valid @RequestBody ClienteDto.Alterar dados) {
        return service.alterar(id, dados);
    }

    @PostMapping("/{id}/encerramento")
    public ClienteDto.Resposta encerrar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return service.encerrar(id, dados);
    }

    @PostMapping("/{id}/reativacao")
    public ClienteDto.Resposta reativar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return service.reativar(id, dados);
    }
}
