package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.services.PedidoSaidaService;
import br.com.rodogarcia.wms.services.PedidoSaidaXmlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos-saida")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class PedidoSaidaController {
    private final PedidoSaidaService pedidos;
    private final PedidoSaidaXmlService xml;

    public PedidoSaidaController(PedidoSaidaService pedidos, PedidoSaidaXmlService xml) {
        this.pedidos = pedidos;
        this.xml = xml;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoSaidaDto.Confirmacao criar(@RequestBody @Valid PedidoSaidaDto.Criar dados) {
        return pedidos.criar(dados);
    }

    @PostMapping("/xml")
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoSaidaDto.ConfirmacaoXml importarXml(
            @RequestBody @Valid PedidoSaidaDto.ImportarXml dados) {
        return xml.importar(dados);
    }

    @GetMapping
    public PaginaResponse<PedidoSaidaDto.Detalhe> listar(
            @RequestParam @Positive Long clienteId,
            @RequestParam @Positive Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return pedidos.listar(clienteId, armazemId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public PedidoSaidaDto.Detalhe consultar(@PathVariable @Positive Long id) {
        return pedidos.consultar(id);
    }

    @GetMapping("/{id}/fifo")
    public PedidoSaidaDto.Sugestao sugerir(@PathVariable @Positive Long id) {
        return pedidos.sugerir(id);
    }

    @PostMapping("/{id}/justificativas-fifo")
    public PedidoSaidaDto.Confirmacao justificar(
            @PathVariable @Positive Long id, @RequestBody @Valid PedidoSaidaDto.Justificar dados) {
        return pedidos.justificar(id, dados);
    }

    @PostMapping("/{id}/reserva")
    public PedidoSaidaDto.Confirmacao reservar(
            @PathVariable @Positive Long id, @RequestBody @Valid PedidoSaidaDto.Reservar dados) {
        return pedidos.reservar(id, dados);
    }

    @PostMapping("/{id}/cancelamento")
    public PedidoSaidaDto.Confirmacao cancelar(
            @PathVariable @Positive Long id, @RequestBody @Valid PedidoSaidaDto.Comando dados) {
        return pedidos.cancelar(id, dados);
    }

    @PostMapping("/{id}/reversao-reserva")
    public PedidoSaidaDto.Confirmacao reverter(
            @PathVariable @Positive Long id, @RequestBody @Valid PedidoSaidaDto.Comando dados) {
        return pedidos.reverter(id, dados);
    }

    @PostMapping("/{id}/revalidacao")
    public PedidoSaidaDto.Detalhe revalidar(@PathVariable @Positive Long id) {
        return pedidos.revalidar(id);
    }
}
