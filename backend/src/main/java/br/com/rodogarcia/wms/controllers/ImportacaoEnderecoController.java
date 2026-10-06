package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.ImportacaoEnderecoDto;
import br.com.rodogarcia.wms.services.ImportacaoEnderecoService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ImportacaoEnderecoController {
    private final ImportacaoEnderecoService service;

    public ImportacaoEnderecoController(ImportacaoEnderecoService service) {
        this.service = service;
    }

    @PostMapping(
            value = "/armazens/{id}/importacoes-enderecos/previa",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacaoEnderecoDto.Resultado previa(
            @PathVariable Long id,
            @RequestPart("arquivo") MultipartFile arquivo,
            @Valid @RequestPart("comando") ImportacaoEnderecoDto.Previa comando)
            throws IOException {
        return service.previa(id, arquivo.getBytes(), comando);
    }

    @GetMapping("/importacoes-enderecos/{id}")
    public ImportacaoEnderecoDto.Resultado consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @PostMapping("/importacoes-enderecos/{id}/confirmacao")
    public ImportacaoEnderecoDto.Resultado confirmar(
            @PathVariable Long id, @Valid @RequestBody ImportacaoEnderecoDto.Confirmar d) {
        return service.confirmar(id, d);
    }
}
