package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class NotaEntradaService {
    private final PedidoEntradaService pedidos;
    private final NotaEntradaRepository notas;
    private final ItemNotaEntradaRepository itens;
    private final ProdutoRepository produtos;
    private final NfeXmlService xml;
    private final Validator validator;

    public NotaEntradaService(
            PedidoEntradaService pedidos,
            NotaEntradaRepository notas,
            ItemNotaEntradaRepository itens,
            ProdutoRepository produtos,
            NfeXmlService xml,
            Validator validator) {
        this.pedidos = pedidos;
        this.notas = notas;
        this.itens = itens;
        this.produtos = produtos;
        this.xml = xml;
        this.validator = validator;
    }

    public PedidoEntradaDto.Resumo adicionar(
            @NotNull @Positive Long id, @NotNull @Valid PedidoEntradaDto.NotaManual dados) {
        var p = pedidos.bloquear(id);
        PedidoEntradaService.versao(p, dados.versao());
        PedidoEntradaService.exigirSituacao(p, SituacaoPedidoEntrada.RASCUNHO);
        pedidos.validarVinculos(p);
        var antes = pedidos.resumo(p);
        var nota = criarNota(p, dados);
        return pedidos.registrar(
                p,
                antes,
                "NOTA_INCLUIDA",
                "Nota de entrada manual",
                Map.of("notaId", nota.getId(), "itens", dados.itens()));
    }

    public PedidoEntradaDto.Resumo importar(
            @NotNull @Positive Long id, @NotNull @Valid PedidoEntradaDto.ImportarXml dados) {
        var p = pedidos.bloquear(id);
        pedidos.validarVinculos(p);
        var nfe = xml.ler(dados.xml());
        if (!p.getCliente().getDocumentoFiscal().equals(nfe.emitente()))
            throw CadastroSupport.invalido(
                    "Emitente do XML deve corresponder ao cliente proprietário.");
        var existente =
                notas.findByEmitenteAndSerieAndNumero(nfe.emitente(), nfe.serie(), nfe.numero());
        String hash = NfeXmlService.hash(dados.xml());
        if (existente.isPresent() && !existente.get().getPedido().getId().equals(id))
            throw RegraNegocioException.conflito(
                    "NOTA_JA_VINCULADA", "Nota já vinculada a um pedido.");
        if (existente.isPresent() && hash.equals(existente.get().getXmlHash()))
            return pedidos.resumo(p);
        PedidoEntradaService.versao(p, dados.versao());
        PedidoEntradaService.exigirSituacao(
                p,
                SituacaoPedidoEntrada.RASCUNHO,
                SituacaoPedidoEntrada.EM_CONFERENCIA,
                SituacaoPedidoEntrada.QUARENTENA,
                SituacaoPedidoEntrada.EFETIVADO);
        var itensXml =
                nfe.itens().stream()
                        .map(
                                i -> {
                                    Long produtoId =
                                            produtos.buscarIdPorSku(p.getCliente().getId(), i.sku())
                                                    .orElseThrow(
                                                            () ->
                                                                    CadastroSupport.invalido(
                                                                            "SKU do XML não cadastrado para o cliente."));
                                    return new PedidoEntradaDto.ItemNota(
                                            i.numeroItem(),
                                            produtoId,
                                            i.quantidade(),
                                            i.valorMercadoria());
                                })
                        .toList();
        var manual =
                new PedidoEntradaDto.NotaManual(
                        dados.versao(),
                        nfe.serie(),
                        nfe.numero(),
                        nfe.emissao(),
                        nfe.chaveAcesso(),
                        itensXml);
        validar(manual);
        var bloqueados = bloquearProdutos(p, itensXml);
        for (int i = 0; i < itensXml.size(); i++) {
            if (!bloqueados
                    .get(itensXml.get(i).produtoId())
                    .getUnidadeMedida()
                    .equals(nfe.itens().get(i).unidade()))
                throw CadastroSupport.invalido(
                        "Unidade comercial do XML difere da unidade do produto. Conversão automática não disponível.");
        }
        var antes = pedidos.resumo(p);
        NotaEntrada nota;
        List<PedidoEntradaDto.ItemNota> valoresAntes = List.of();
        if (existente.isPresent()) {
            nota = existente.get();
            if (nota.getXmlHash() != null
                    || !nota.getEmissao().equals(nfe.emissao())
                    || (nota.getChaveAcesso() != null
                            && !nota.getChaveAcesso().equals(nfe.chaveAcesso())))
                throw xmlDivergente();
            var cadastrados = itens.findByNotaIdOrderByNumeroItem(nota.getId());
            valoresAntes =
                    cadastrados.stream()
                            .map(
                                    i ->
                                            new PedidoEntradaDto.ItemNota(
                                                    i.getNumeroItem(),
                                                    i.getProduto().getId(),
                                                    i.getQuantidadePrevista(),
                                                    i.getValorMercadoria()))
                            .toList();
            if (cadastrados.size() != itensXml.size()) throw xmlDivergente();
            var porNumero = new HashMap<Integer, PedidoEntradaDto.ItemNota>();
            itensXml.forEach(i -> porNumero.put(i.numeroItem(), i));
            for (var i : cadastrados) {
                var recebido = porNumero.get(i.getNumeroItem());
                if (recebido == null
                        || !recebido.produtoId().equals(i.getProduto().getId())
                        || recebido.quantidadePrevista().compareTo(i.getQuantidadePrevista()) != 0
                        || (i.getValorMercadoria() != null
                                && recebido.valorMercadoria().compareTo(i.getValorMercadoria())
                                        != 0)) throw xmlDivergente();
                i.complementarValor(recebido.valorMercadoria());
            }
            if (nota.getChaveAcesso() == null && notas.existsByChaveAcesso(nfe.chaveAcesso()))
                throw xmlDivergente();
        } else {
            PedidoEntradaService.exigirSituacao(p, SituacaoPedidoEntrada.RASCUNHO);
            nota = criarNota(p, manual);
        }
        nota.vincularXml(nfe.chaveAcesso(), hash, dados.xml());
        notas.flush();
        return pedidos.registrar(
                p,
                antes,
                "XML_VINCULADO",
                "XML vinculado à nota de entrada",
                Map.of(
                        "notaId",
                        nota.getId(),
                        "xmlHash",
                        hash,
                        "itensAntes",
                        valoresAntes,
                        "itensXml",
                        itensXml));
    }

    /** Criação mapeada pelo serviço XML, na mesma transação do pedido e do recibo. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void adicionarXmlAssociado(Long id, PedidoEntradaDto.NotaManual dados, String original) {
        var p = pedidos.bloquear(id);
        PedidoEntradaService.versao(p, dados.versao());
        PedidoEntradaService.exigirSituacao(p, SituacaoPedidoEntrada.RASCUNHO);
        pedidos.validarVinculos(p);
        var antes = pedidos.resumo(p);
        var nota = criarNota(p, dados);
        nota.vincularXml(dados.chaveAcesso(), NfeXmlService.hash(original), original);
        notas.flush();
        pedidos.registrar(
                p,
                antes,
                "XML_VINCULADO",
                "Nota importada na criação do pedido",
                Map.of(
                        "notaId",
                        nota.getId(),
                        "xmlHash",
                        nota.getXmlHash(),
                        "associacoes",
                        dados.itens()));
    }

    private NotaEntrada criarNota(PedidoEntrada p, PedidoEntradaDto.NotaManual dados) {
        validar(dados);
        if (notas.countByPedidoId(p.getId()) >= 20
                || itens.countByNotaPedidoId(p.getId()) + dados.itens().size() > 1000)
            throw CadastroSupport.invalido("Limite de 20 notas ou 1000 itens por pedido excedido.");
        if (notas.findByEmitenteAndSerieAndNumero(
                                p.getCliente().getDocumentoFiscal(), dados.serie(), dados.numero())
                        .isPresent()
                || (dados.chaveAcesso() != null && notas.existsByChaveAcesso(dados.chaveAcesso())))
            throw RegraNegocioException.conflito(
                    "NOTA_JA_VINCULADA", "Nota já vinculada a um pedido.");
        var cadastrados = bloquearProdutos(p, dados.itens());
        var nota =
                notas.saveAndFlush(
                        new NotaEntrada(
                                p,
                                p.getCliente().getDocumentoFiscal(),
                                dados.serie(),
                                dados.numero(),
                                dados.emissao(),
                                dados.chaveAcesso()));
        for (var item : dados.itens())
            itens.save(
                    new ItemNotaEntrada(
                            nota,
                            item.numeroItem(),
                            cadastrados.get(item.produtoId()),
                            item.quantidadePrevista(),
                            item.valorMercadoria()));
        itens.flush();
        return nota;
    }

    private Map<Long, Produto> bloquearProdutos(
            PedidoEntrada p, List<PedidoEntradaDto.ItemNota> dados) {
        var numeros = new HashSet<Integer>();
        var cadastrados = new HashMap<Long, Produto>();
        for (var item : dados)
            if (!numeros.add(item.numeroItem()))
                throw CadastroSupport.invalido("Número de item repetido na nota.");
        for (Long id :
                dados.stream()
                        .map(PedidoEntradaDto.ItemNota::produtoId)
                        .distinct()
                        .sorted()
                        .toList()) {
            var produto =
                    produtos.buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!Objects.equals(produto.getCliente().getId(), p.getCliente().getId()))
                throw CadastroSupport.invalido("Produto não pertence ao cliente do pedido.");
            CadastroSupport.ativo(produto);
            cadastrados.put(id, produto);
        }
        for (var item : dados)
            CadastroSupport.quantidade(
                    cadastrados.get(item.produtoId()), item.quantidadePrevista());
        return cadastrados;
    }

    private void validar(PedidoEntradaDto.NotaManual dados) {
        var violacoes = validator.validate(dados);
        if (!violacoes.isEmpty()) throw new ConstraintViolationException(violacoes);
    }

    private RegraNegocioException xmlDivergente() {
        return RegraNegocioException.conflito(
                "XML_DIVERGENTE",
                "XML difere da nota registrada ou já existe um XML vinculado. Preserve a conferência e trate a divergência.");
    }
}
