package br.com.rodogarcia.wms.controllers;

import br.com.rodogarcia.wms.dto.ConfiguracaoCobrancaDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.services.ConfiguracaoCobrancaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ConfiguracaoCobrancaController {
    private final ConfiguracaoCobrancaService service;

    public ConfiguracaoCobrancaController(ConfiguracaoCobrancaService service) {
        this.service = service;
    }

    @GetMapping("/servicos-cobranca")
    public PaginaResponse<ConfiguracaoCobrancaDto.Servico> servicos(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho) {
        return service.servicos(pagina, tamanho);
    }

    @PostMapping("/servicos-cobranca")
    public ConfiguracaoCobrancaDto.Servico criarServico(
            @RequestBody @Valid ConfiguracaoCobrancaDto.CriarServico d) {
        return service.criarServico(d);
    }

    @GetMapping("/tabelas-cobranca")
    public List<ConfiguracaoCobrancaDto.Tabela> tabelas(@RequestParam Long armazemId) {
        return service.tabelas(armazemId);
    }

    @GetMapping("/tabelas-cobranca/{id}")
    public ConfiguracaoCobrancaDto.Tabela tabela(@PathVariable Long id) {
        return service.tabela(id);
    }

    @PostMapping("/tabelas-cobranca")
    public ConfiguracaoCobrancaDto.Tabela criarTabela(
            @RequestBody @Valid ConfiguracaoCobrancaDto.CriarTabela d) {
        return service.criarTabela(d);
    }

    @PostMapping("/tabelas-cobranca/{id}/encerramento")
    public ConfiguracaoCobrancaDto.Tabela encerrarTabela(
            @PathVariable Long id, @RequestBody @Valid ConfiguracaoCobrancaDto.Encerrar d) {
        return service.encerrarTabela(id, d);
    }

    @GetMapping("/vinculos-tabela")
    public List<ConfiguracaoCobrancaDto.Vinculo> vinculos(
            @RequestParam Long clienteId, @RequestParam Long armazemId) {
        return service.vinculos(clienteId, armazemId);
    }

    @PostMapping("/vinculos-tabela")
    public ConfiguracaoCobrancaDto.Vinculo vincular(
            @RequestBody @Valid ConfiguracaoCobrancaDto.Vincular d) {
        return service.vincular(d);
    }

    @PostMapping("/vinculos-tabela/{id}/encerramento")
    public ConfiguracaoCobrancaDto.Vinculo encerrarVinculo(
            @PathVariable Long id, @RequestBody @Valid ConfiguracaoCobrancaDto.Encerrar d) {
        return service.encerrarVinculo(id, d);
    }

    @GetMapping("/contratos-cobranca")
    public List<ConfiguracaoCobrancaDto.Contrato> contratos(
            @RequestParam Long clienteId, @RequestParam Long armazemId) {
        return service.contratos(clienteId, armazemId);
    }

    @PostMapping("/contratos-cobranca")
    public ConfiguracaoCobrancaDto.Contrato configurar(
            @RequestBody @Valid ConfiguracaoCobrancaDto.ConfigurarContrato d) {
        return service.configurar(d);
    }

    @PostMapping("/contratos-cobranca/{id}/encerramento")
    public ConfiguracaoCobrancaDto.Contrato encerrarContrato(
            @PathVariable Long id, @RequestBody @Valid ConfiguracaoCobrancaDto.Encerrar d) {
        return service.encerrarContrato(id, d);
    }
}
