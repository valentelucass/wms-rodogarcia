package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.services.NotaEntradaService;
import br.com.rodogarcia.wms.services.PedidoEntradaService;
import br.com.rodogarcia.wms.services.RecebimentoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pedidos-entrada")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class PedidoEntradaController {
    private final PedidoEntradaService pedidos;
    private final NotaEntradaService notas;
    private final RecebimentoService recebimento;

    public PedidoEntradaController(
            PedidoEntradaService pedidos,
            NotaEntradaService notas,
            RecebimentoService recebimento) {
        this.pedidos = pedidos;
        this.notas = notas;
        this.recebimento = recebimento;
    }

    @PostMapping
    public ResponseEntity<PedidoEntradaDto.Resumo> criar(
            @Valid @RequestBody PedidoEntradaDto.Criar dados) {
        var resultado = pedidos.criar(dados);
        return ResponseEntity.created(URI.create("/api/v1/pedidos-entrada/" + resultado.id()))
                .body(resultado);
    }

    @GetMapping
    public PaginaResponse<PedidoEntradaDto.Resumo> listar(
            @RequestParam @Positive Long clienteId,
            @RequestParam @Positive Long armazemId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return pedidos.listar(clienteId, armazemId, pagina, tamanho);
    }

    @GetMapping("/{id}")
    public PedidoEntradaDto.Detalhe consultar(@PathVariable @Positive Long id) {
        return pedidos.consultar(id);
    }

    @PostMapping("/{id}/notas")
    public PedidoEntradaDto.Resumo nota(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PedidoEntradaDto.NotaManual dados) {
        return notas.adicionar(id, dados);
    }

    @PostMapping("/{id}/notas/xml")
    public PedidoEntradaDto.Resumo xml(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PedidoEntradaDto.ImportarXml dados) {
        return notas.importar(id, dados);
    }

    @PostMapping("/{id}/iniciar-conferencia")
    public PedidoEntradaDto.Resumo iniciar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return pedidos.iniciar(id, dados);
    }

    @PostMapping("/{id}/chegadas")
    public PedidoEntradaDto.Resumo chegada(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RecebimentoDto.RegistrarChegada dados) {
        return recebimento.registrarChegada(id, dados);
    }

    @PostMapping("/{id}/chegadas/{chegadaId}/estorno")
    public PedidoEntradaDto.Resumo estornar(
            @PathVariable @Positive Long id,
            @PathVariable @Positive Long chegadaId,
            @Valid @RequestBody RevisaoCadastroRequest dados) {
        return recebimento.estornar(id, chegadaId, dados);
    }

    @PostMapping("/{id}/efetivacao")
    public PedidoEntradaDto.Resumo efetivar(
            @PathVariable @Positive Long id, @Valid @RequestBody PedidoEntradaDto.Efetivar dados) {
        return recebimento.efetivar(id, dados);
    }

    @PostMapping("/{id}/cancelamento")
    public PedidoEntradaDto.Resumo cancelar(
            @PathVariable @Positive Long id, @Valid @RequestBody RevisaoCadastroRequest dados) {
        return pedidos.cancelar(id, dados);
    }

    @GetMapping("/{id}/chegadas")
    public PaginaResponse<RecebimentoDto.Chegada> chegadas(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return recebimento.listarChegadas(id, pagina, tamanho);
    }

    @GetMapping("/{id}/entradas")
    public PaginaResponse<RecebimentoDto.Entrada> entradas(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return recebimento.listarEntradas(id, pagina, tamanho);
    }
}
