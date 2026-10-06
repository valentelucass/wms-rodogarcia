package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.services.EnderecoService;
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
@RequestMapping("/api/v1/enderecos")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class EnderecoController {
    private final EnderecoService service;

    public EnderecoController(EnderecoService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<EnderecoDto.Resposta> listar(
            @RequestParam @Positive Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(armazemId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public EnderecoDto.Resposta consultar(@PathVariable @Positive Long id) {
        return service.consultar(id);
    }

    @PostMapping
    public ResponseEntity<EnderecoDto.Resposta> criar(@Valid @RequestBody EnderecoDto.Criar dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/enderecos/" + resposta.id()))
                .body(resposta);
    }

    @PutMapping("/{id}")
    public EnderecoDto.Resposta alterar(
            @PathVariable @Positive Long id, @Valid @RequestBody EnderecoDto.Alterar dados) {
        return service.alterar(id, dados);
    }

    @PostMapping("/{id}/encerramento")
    public EnderecoDto.Resposta encerrar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return service.encerrar(id, dados);
    }

    @PostMapping("/{id}/reativacao")
    public EnderecoDto.Resposta reativar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return service.reativar(id, dados);
    }
}
