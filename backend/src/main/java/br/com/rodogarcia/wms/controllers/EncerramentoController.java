package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.EncerramentoDto;
import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.services.EncerramentoService;
import br.com.rodogarcia.wms.services.PedidoSaidaService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/encerramentos")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class EncerramentoController {
    private final EncerramentoService service;
    private final PedidoSaidaService pedidos;

    public EncerramentoController(EncerramentoService service, PedidoSaidaService pedidos) {
        this.service = service;
        this.pedidos = pedidos;
    }

    @GetMapping("/{tipo}/{id}/impedimentos")
    public EncerramentoDto.Resultado consultar(
            @PathVariable EncerramentoDto.Tipo tipo, @PathVariable Long id) {
        return service.consultar(tipo, id);
    }

    @PostMapping("/{tipo}/{id}/solicitar")
    public EncerramentoDto.Resultado solicitar(
            @PathVariable EncerramentoDto.Tipo tipo,
            @PathVariable Long id,
            @RequestBody @Valid EncerramentoDto.Confirmar d) {
        return service.solicitar(tipo, id, d);
    }

    @PostMapping("/{tipo}/{id}/inativar")
    public EncerramentoDto.Resultado inativar(
            @PathVariable EncerramentoDto.Tipo tipo,
            @PathVariable Long id,
            @RequestBody @Valid EncerramentoDto.Confirmar d) {
        return service.inativar(tipo, id, d);
    }

    @PostMapping("/remanescente")
    public PedidoSaidaDto.Confirmacao remanescente(
            @RequestBody @Valid EncerramentoDto.Remanescente d) {
        return pedidos.resolverRemanescente(d);
    }

    @PostMapping("/vigencias/{tipo}/{id}/encerrar")
    public EncerramentoDto.Vigencia vigencia(
            @PathVariable String tipo,
            @PathVariable Long id,
            @RequestBody @Valid EncerramentoDto.EncerrarVigencia d) {
        return service.encerrarVigencia(tipo, id, d);
    }
}
