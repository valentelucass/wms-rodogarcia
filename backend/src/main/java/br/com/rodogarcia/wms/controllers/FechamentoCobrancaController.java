package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.ConfirmacaoComando;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Confirmar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Decidir;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Entregar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Fechamento;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Nfse;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Preparar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Reabrir;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Resolver;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Tratar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Tratativa;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Versao;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.FechamentoCobrancaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fechamentos-cobranca")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FechamentoCobrancaController {
    private final FechamentoCobrancaService service;

    public FechamentoCobrancaController(FechamentoCobrancaService service) {
        this.service = service;
    }

    @GetMapping
    public PaginaResponse<Fechamento> listar(
            @RequestParam Long clienteId,
            @RequestParam Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.listar(clienteId, armazemId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public Fechamento consultar(@PathVariable Long id) {
        return service.consultar(id);
    }

    @GetMapping("/{id}/versoes")
    public List<Versao> versoes(@PathVariable Long id) {
        return service.versoes(id);
    }

    @GetMapping("/{id}/versoes/{numero}")
    public Versao versao(@PathVariable Long id, @PathVariable int numero) {
        return service.versao(id, numero);
    }

    @GetMapping(
            value = "/{id}/versoes/{numero}/demonstrativo",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> demonstrativo(@PathVariable Long id, @PathVariable int numero) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(service.demonstrativo(id, numero));
    }

    @GetMapping("/{id}/tratativas-externas")
    public List<Tratativa> tratativas(@PathVariable Long id) {
        return service.tratativas(id);
    }

    @PostMapping("/{id}/tratativas-externas")
    public ConfirmacaoComando tratar(@PathVariable Long id, @RequestBody @Valid Tratar d) {
        return service.tratarExterno(id, d);
    }

    @PostMapping
    public ConfirmacaoComando preparar(@RequestBody @Valid Preparar d) {
        return service.preparar(d);
    }

    @PostMapping("/{id}/aprovacao")
    public ConfirmacaoComando aprovar(@PathVariable Long id, @RequestBody @Valid Decidir d) {
        return service.aprovar(id, d);
    }

    @PostMapping("/{id}/rejeicao")
    public ConfirmacaoComando rejeitar(@PathVariable Long id, @RequestBody @Valid Decidir d) {
        return service.rejeitar(id, d);
    }

    @PostMapping("/{id}/reabertura")
    public ConfirmacaoComando reabrir(@PathVariable Long id, @RequestBody @Valid Reabrir d) {
        return service.reabrir(id, d);
    }

    @PostMapping("/{id}/entregas")
    public ConfirmacaoComando entregar(@PathVariable Long id, @RequestBody @Valid Entregar d) {
        return service.entregar(id, d);
    }

    @PostMapping("/{id}/confirmacoes-externas")
    public ConfirmacaoComando confirmarNaoEmissao(
            @PathVariable Long id, @RequestBody @Valid Confirmar d) {
        return service.confirmarNaoEmissao(id, d);
    }

    @PostMapping("/{id}/referencias-nfse")
    public ConfirmacaoComando registrarNfse(@PathVariable Long id, @RequestBody @Valid Nfse d) {
        return service.registrarNfse(id, d);
    }

    @PostMapping("/{id}/resolucao-financeira")
    public ConfirmacaoComando resolverSaldo(@PathVariable Long id, @RequestBody @Valid Resolver d) {
        return service.resolverSaldo(id, d);
    }
}
