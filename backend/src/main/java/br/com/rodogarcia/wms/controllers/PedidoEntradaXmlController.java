package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.NfeImportacaoDto;
import br.com.rodogarcia.wms.services.PedidoEntradaXmlService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class PedidoEntradaXmlController {
    private final PedidoEntradaXmlService service;

    public PedidoEntradaXmlController(PedidoEntradaXmlService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/pedidos-entrada/xml/previa")
    public NfeImportacaoDto.Previa previa(@Valid @RequestBody NfeImportacaoDto.Ler dados) {
        return service.previa(dados);
    }

    @PostMapping("/api/v1/pedidos-entrada/xml/confirmacao")
    public ResponseEntity<NfeImportacaoDto.Confirmacao> confirmar(
            @Valid @RequestBody NfeImportacaoDto.Confirmar dados) {
        var resposta = service.confirmar(dados);
        return ResponseEntity.created(
                        URI.create("/api/v1/pedidos-entrada/" + resposta.pedido().id()))
                .body(resposta);
    }

    @GetMapping("/api/v1/pedidos-entrada/xml/operacoes/{operacaoId}")
    public NfeImportacaoDto.Confirmacao resultado(@PathVariable UUID operacaoId) {
        return service.resultado(operacaoId);
    }

    @GetMapping("/api/v1/pedidos-entrada/{pedidoId}/notas/{notaId}/documento")
    public NfeImportacaoDto.DocumentoSalvo documento(
            @PathVariable Long pedidoId, @PathVariable Long notaId) {
        return service.documento(pedidoId, notaId);
    }
}
