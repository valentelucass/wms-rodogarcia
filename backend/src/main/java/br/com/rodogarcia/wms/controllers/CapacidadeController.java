package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.CapacidadeDto;
import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.services.CapacidadeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class CapacidadeController {
    private final CapacidadeService service;

    public CapacidadeController(CapacidadeService service) {
        this.service = service;
    }

    @PutMapping("/api/v1/enderecos/{id}/capacidade")
    public EnderecoDto.Resposta configurar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CapacidadeDto.ConfigurarEndereco dados) {
        return service.configurar(id, dados);
    }

    @PostMapping("/api/v1/conjuntos-posicoes")
    public ResponseEntity<CapacidadeDto.Conjunto> criar(
            @Valid @RequestBody CapacidadeDto.CriarConjunto dados) {
        var resposta = service.criar(dados);
        return ResponseEntity.created(
                        URI.create("/api/v1/conjuntos-posicoes?armazemId=" + resposta.armazemId()))
                .body(resposta);
    }

    @GetMapping("/api/v1/conjuntos-posicoes")
    public PaginaResponse<CapacidadeDto.Conjunto> listar(
            @RequestParam @Positive Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(armazemId, pagina, tamanho);
    }

    @PostMapping("/api/v1/conjuntos-posicoes/{id}/encerramento")
    public CapacidadeDto.Conjunto encerrar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return service.encerrar(id, dados);
    }
}
