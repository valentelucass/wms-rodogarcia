package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.services.UnidadeLogisticaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class UnidadeLogisticaController {
    private final UnidadeLogisticaService unidades;

    public UnidadeLogisticaController(UnidadeLogisticaService unidades) {
        this.unidades = unidades;
    }

    @PostMapping("/api/v1/pedidos-entrada/{pedidoId}/entradas/{entradaId}/unitizacao")
    public UnidadeLogisticaDto.Resultado unitizar(
            @PathVariable @Positive Long pedidoId,
            @PathVariable @Positive Long entradaId,
            @Valid @RequestBody UnidadeLogisticaDto.Unitizar dados) {
        return unidades.unitizar(pedidoId, entradaId, dados);
    }

    @PostMapping("/api/v1/pedidos-entrada/{pedidoId}/unidades/{unidadeId}/divisao")
    public UnidadeLogisticaDto.Resultado dividir(
            @PathVariable @Positive Long pedidoId,
            @PathVariable @Positive Long unidadeId,
            @Valid @RequestBody UnidadeLogisticaDto.Dividir dados) {
        return unidades.dividir(pedidoId, unidadeId, dados);
    }

    @PostMapping("/api/v1/pedidos-entrada/{pedidoId}/unidades/{unidadeId}/reagrupamento")
    public UnidadeLogisticaDto.Resultado reagrupar(
            @PathVariable @Positive Long pedidoId,
            @PathVariable @Positive Long unidadeId,
            @Valid @RequestBody UnidadeLogisticaDto.Reagrupar dados) {
        return unidades.reagrupar(pedidoId, unidadeId, dados);
    }

    @GetMapping("/api/v1/pedidos-entrada/{pedidoId}/unitizacao")
    public UnidadeLogisticaDto.Progresso progresso(@PathVariable @Positive Long pedidoId) {
        return unidades.progresso(pedidoId);
    }

    @GetMapping("/api/v1/pedidos-entrada/{pedidoId}/unidades")
    public PaginaResponse<UnidadeLogisticaDto.Resumo> listar(
            @PathVariable @Positive Long pedidoId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return unidades.listar(pedidoId, pagina, tamanho);
    }

    @GetMapping("/api/v1/pedidos-entrada/{pedidoId}/unidades/{unidadeId}")
    public UnidadeLogisticaDto.Detalhe consultar(
            @PathVariable @Positive Long pedidoId, @PathVariable @Positive Long unidadeId) {
        return unidades.consultar(pedidoId, unidadeId);
    }

    @GetMapping("/api/v1/unidades-logisticas/{codigo}")
    public UnidadeLogisticaDto.Detalhe lerCodigo(@PathVariable UUID codigo) {
        return unidades.lerCodigo(codigo);
    }

    @GetMapping("/api/v1/unidades-logisticas/{codigo}/etiqueta")
    public UnidadeLogisticaDto.Etiqueta etiqueta(@PathVariable UUID codigo) {
        return unidades.etiqueta(codigo);
    }
}
