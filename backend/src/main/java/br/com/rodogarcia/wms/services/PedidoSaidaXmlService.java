package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Criação integral de pedido pela origem XML; documento e retirada continuam independentes. */
@Service
@Validated
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class PedidoSaidaXmlService {
    private final PedidoSaidaService pedidos;
    private final PedidoSaidaRepository repository;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final NfeXmlService xml;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;

    public PedidoSaidaXmlService(
            PedidoSaidaService pedidos,
            PedidoSaidaRepository repository,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            NfeXmlService xml,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso) {
        this.pedidos = pedidos;
        this.repository = repository;
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.xml = xml;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
    }

    @Transactional
    public PedidoSaidaDto.ConfirmacaoXml importar(
            @NotNull @Valid PedidoSaidaDto.ImportarXml dados) {
        acesso.cliente(dados.clienteId());
        acesso.armazem(dados.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(dados.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        String hash = operacoes.hash("PEDIDO_SAIDA_XML", null, dados);
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, PedidoSaidaDto.ConfirmacaoXml.class);
        if (repetida != null) return repetida;
        CadastroSupport.ativo(cliente);
        CadastroSupport.ativo(armazem);
        var documento = xml.ler(dados.xml());
        if (documento.serie() < 0
                || documento.serie() > 999
                || documento.numero() < 1
                || documento.numero() > 999999999)
            throw CadastroSupport.invalido("Identificação da nota inválida.");
        String origem = documento.emitente() + ":" + documento.serie() + ":" + documento.numero();
        String referencia =
                "XML-"
                        + OperacaoAdministrativaService.digest(
                                        origem.getBytes(StandardCharsets.UTF_8))
                                .substring(0, 36)
                                .toUpperCase(java.util.Locale.ROOT);
        if (repository
                .findByClienteIdAndArmazemIdAndReferencia(
                        dados.clienteId(), dados.armazemId(), referencia)
                .isPresent())
            throw RegraNegocioException.conflito(
                    "REFERENCIA_DUPLICADA", "Nota já utilizada por outro pedido neste contexto.");
        var quantidades = new TreeMap<Long, BigDecimal>();
        var unidades = new HashMap<Long, HashSet<String>>();
        for (var item : documento.itens()) {
            if (item.quantidade().signum() <= 0
                    || (long) item.quantidade().precision() - item.quantidade().scale() > 13
                    || item.quantidade().stripTrailingZeros().scale() > 6
                    || item.valorMercadoria().signum() < 0)
                throw CadastroSupport.invalido("Quantidade ou valor da linha XML inválido.");
            Long id =
                    produtos.buscarIdPorSku(cliente.getId(), item.sku())
                            .orElseThrow(
                                    () ->
                                            CadastroSupport.invalido(
                                                    "SKU não cadastrado para este cliente."));
            quantidades.merge(id, item.quantidade(), BigDecimal::add);
            unidades.computeIfAbsent(id, ignorado -> new HashSet<>()).add(item.unidade());
        }
        var itens =
                quantidades.entrySet().stream()
                        .map(
                                e -> {
                                    var produto =
                                            produtos.buscarParaAtualizar(e.getKey())
                                                    .orElseThrow(
                                                            RegraNegocioException::naoEncontrado);
                                    CadastroSupport.ativo(produto);
                                    if (!produto.getCliente().getId().equals(cliente.getId())
                                            || !unidades.get(e.getKey())
                                                    .equals(
                                                            java.util.Set.of(
                                                                    produto.getUnidadeMedida())))
                                        throw CadastroSupport.invalido(
                                                "SKU ou unidade XML incompatível com o cadastro do cliente.");
                                    CadastroSupport.quantidade(produto, e.getValue());
                                    return new PedidoSaidaDto.ItemCriar(e.getKey(), e.getValue());
                                })
                        .toList();
        var criado =
                pedidos.criar(
                        new PedidoSaidaDto.Criar(
                                dados.operacaoId(),
                                cliente.getId(),
                                armazem.getId(),
                                referencia,
                                itens,
                                dados.motivo()));
        var resultado =
                new PedidoSaidaDto.ConfirmacaoXml(
                        dados.operacaoId(), NfeXmlService.hash(dados.xml()), documento, criado.pedido());
        operacoes.salvar(
                dados.operacaoId(),
                "CRIACAO",
                cliente,
                armazem,
                criado.pedido().id(),
                hash,
                resultado);
        auditoria.registrar(
                "PEDIDO_SAIDA",
                criado.pedido().id(),
                "XML_VINCULADO",
                dados.motivo(),
                null,
                resultado);
        return resultado;
    }
}
