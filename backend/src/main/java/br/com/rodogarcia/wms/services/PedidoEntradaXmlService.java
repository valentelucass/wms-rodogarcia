package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.NfeImportacaoDto;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoAdministrativaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
@Transactional(readOnly = true)
public class PedidoEntradaXmlService {
    private final NfeDocumentoService documentos;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final NotaEntradaRepository notas;
    private final OperacaoAdministrativaRepository recibos;
    private final OperacaoAdministrativaService operacoes;
    private final PedidoEntradaService pedidos;
    private final NotaEntradaService inclusao;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final String ambiente;

    public PedidoEntradaXmlService(
            NfeDocumentoService documentos,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            NotaEntradaRepository notas,
            OperacaoAdministrativaRepository recibos,
            OperacaoAdministrativaService operacoes,
            PedidoEntradaService pedidos,
            NotaEntradaService inclusao,
            AcessoService acesso,
            JsonMapper mapper,
            @Value("${wms.recebimento.xml.ambiente:1}") String ambiente) {
        this.documentos = documentos;
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.notas = notas;
        this.recibos = recibos;
        this.operacoes = operacoes;
        this.pedidos = pedidos;
        this.inclusao = inclusao;
        this.acesso = acesso;
        this.mapper = mapper;
        if (!List.of("1", "2").contains(ambiente))
            throw new IllegalArgumentException("Ambiente NF-e inválido");
        this.ambiente = ambiente;
    }

    public NfeImportacaoDto.Previa previa(@NotNull @Valid NfeImportacaoDto.Ler dados) {
        acesso.usuario();
        return analisar(dados, false);
    }

    private NfeImportacaoDto.Previa analisar(NfeImportacaoDto.Ler dados, boolean bloquear) {
        var doc = documentos.ler(dados.xml());
        var pendencias = new ArrayList<String>();
        var avisos = new ArrayList<String>();
        avisos.add(
                "O emitente é o proprietário. O destinatário permanece como informado na nota; o armazém apenas guarda a mercadoria.");
        avisos.add(
                "O arquivo foi conferido pelo esquema local. Assinatura, autenticidade e situação atual da nota não foram consultadas na SEFAZ.");
        Long clienteId = dados.clienteId();
        if (clienteId == null) {
            var candidatos =
                    clientes.findByDocumentoFiscal(doc.emitente().documento()).stream()
                            .filter(c -> c.getSituacao() == SituacaoCadastro.ATIVO)
                            .filter(c -> acesso.gestor() || acesso.clientes().contains(c.getId()))
                            .toList();
            if (candidatos.size() == 1) clienteId = candidatos.getFirst().getId();
        }
        Long armazemId = dados.armazemId();
        if (armazemId == null) {
            var candidatos =
                    armazens.findByDocumentoFiscal(doc.destinatario().documento()).stream()
                            .filter(a -> a.getSituacao() == SituacaoCadastro.ATIVO)
                            .filter(a -> acesso.gestor() || acesso.armazens().contains(a.getId()))
                            .toList();
            if (candidatos.size() == 1) armazemId = candidatos.getFirst().getId();
        }
        Long clienteVersao = null;
        Long armazemVersao = null;
        boolean clienteCompativel = false;
        if (clienteId == null)
            pendencias.add("Selecione o cliente proprietário correspondente ao emitente da nota.");
        else {
            acesso.cliente(clienteId);
            var c =
                    (bloquear
                                    ? clientes.buscarParaAtualizar(clienteId)
                                    : clientes.findById(clienteId))
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            clienteVersao = c.getVersao();
            clienteCompativel = c.getDocumentoFiscal().equals(doc.emitente().documento());
            if (!clienteCompativel)
                pendencias.add("O cliente proprietário deve ter o mesmo CNPJ/CPF do emitente.");
            if (c.getSituacao() != SituacaoCadastro.ATIVO)
                pendencias.add("O cliente proprietário não está ativo.");
        }
        if (armazemId == null)
            pendencias.add("Selecione o armazém autorizado que receberá a mercadoria.");
        else {
            acesso.armazem(armazemId);
            var a =
                    (bloquear
                                    ? armazens.buscarParaAtualizar(armazemId)
                                    : armazens.findById(armazemId))
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            armazemVersao = a.getVersao();
            if (a.getSituacao() != SituacaoCadastro.ATIVO)
                pendencias.add("O armazém não está ativo.");
            if (!Objects.equals(a.getDocumentoFiscal(), doc.destinatario().documento()))
                avisos.add(
                        "O documento do armazém selecionado difere do destinatário. Confira o local físico de recebimento antes de confirmar.");
        }
        if (!ambiente.equals(doc.ambiente()))
            pendencias.add(
                    "O ambiente fiscal da nota difere do ambiente NF-e configurado para importação.");
        if (doc.protocolo() == null) avisos.add("O arquivo não contém protocolo de autorização.");
        else if (!List.of("100", "150").contains(doc.protocolo().codigoSituacao()))
            pendencias.add("O protocolo informado não indica autorização de uso da NF-e.");
        var explicitas = new HashMap<Integer, Long>();
        var numeros = doc.itens().stream().map(NfeImportacaoDto.Item::numeroItem).toList();
        for (var a : dados.associacoes()) {
            if (!numeros.contains(a.numeroItem())
                    || explicitas.put(a.numeroItem(), a.produtoId()) != null)
                throw CadastroSupport.invalido("Associação repetida ou item inexistente no XML.");
        }
        var ids = new HashMap<Integer, Long>();
        for (var i : doc.itens()) {
            Long id = explicitas.get(i.numeroItem());
            if (id == null && clienteCompativel)
                id =
                        produtos.buscarIdPorSku(clienteId, CadastroSupport.codigo(i.codigo()))
                                .orElse(null);
            if (id != null) ids.put(i.numeroItem(), id);
        }
        var catalogo = new HashMap<Long, br.com.rodogarcia.wms.models.Produto>();
        for (Long id : ids.values().stream().distinct().sorted().toList()) {
            var p =
                    (bloquear ? produtos.buscarParaAtualizar(id) : produtos.findById(id))
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!Objects.equals(p.getCliente().getId(), clienteId))
                throw CadastroSupport.invalido(
                        "Produto associado não pertence ao cliente proprietário.");
            catalogo.put(id, p);
        }
        var associados = new ArrayList<NfeImportacaoDto.ItemAssociado>();
        for (var i : doc.itens()) {
            var p = catalogo.get(ids.get(i.numeroItem()));
            String pendencia = null;
            if (p == null) pendencia = "Associe este item a um produto do cliente proprietário.";
            else if (p.getSituacao() != SituacaoCadastro.ATIVO)
                pendencia = "O produto associado não está ativo.";
            else if (!p.getUnidadeMedida().equals(CadastroSupport.codigo(i.unidadeComercial())))
                pendencia =
                        "Unidade comercial diferente da unidade de estoque. Conversão não cadastrada; não é possível confirmar.";
            else {
                try {
                    CadastroSupport.quantidade(p, i.quantidadeComercial());
                } catch (RegraNegocioException e) {
                    pendencia = "Quantidade incompatível com a precisão do produto.";
                }
            }
            if (pendencia != null) pendencias.add("Item " + i.numeroItem() + ": " + pendencia);
            associados.add(
                    new NfeImportacaoDto.ItemAssociado(
                            i.numeroItem(),
                            p == null ? null : p.getId(),
                            p == null ? null : p.getVersao(),
                            p == null ? null : p.getSku(),
                            p == null ? null : p.getDescricao(),
                            p == null ? null : p.getUnidadeMedida(),
                            pendencia == null ? i.quantidadeComercial() : null,
                            pendencia == null ? BigDecimal.ONE : null,
                            pendencia));
        }
        var duplicada =
                notas.findByEmitenteAndSerieAndNumero(
                                doc.emitente().documento(), doc.serie(), doc.numero())
                        .or(() -> notas.findByChaveAcesso(doc.chaveAcesso()))
                        .orElse(null);
        Long pedidoExistente = null;
        if (duplicada != null) {
            pendencias.add("Esta nota já está vinculada a um pedido de entrada.");
            var p = duplicada.getPedido();
            if (acesso.gestor()
                    || (acesso.clientes().contains(p.getCliente().getId())
                            && acesso.armazens().contains(p.getArmazem().getId())))
                pedidoExistente = p.getId();
        }
        String hash = NfeXmlService.hash(dados.xml());
        String revisao =
                operacoes.hash(
                        "PREVIA_ENTRADA_XML",
                        hash,
                        java.util.Arrays.asList(
                                clienteId,
                                clienteVersao,
                                armazemId,
                                armazemVersao,
                                associados,
                                pendencias));
        return new NfeImportacaoDto.Previa(
                doc,
                hash,
                revisao,
                clienteId,
                armazemId,
                List.copyOf(associados),
                List.copyOf(pendencias),
                List.copyOf(avisos),
                pedidoExistente,
                pendencias.isEmpty());
    }

    @Transactional
    public NfeImportacaoDto.Confirmacao confirmar(
            @NotNull @Valid NfeImportacaoDto.Confirmar dados) {
        acesso.cliente(dados.clienteId());
        acesso.armazem(dados.armazemId());
        // O proprietário é o domínio de serialização: confirmação/repetição e duplicidade da nota.
        var cliente =
                clientes.buscarParaAtualizar(dados.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var associacoes =
                dados.associacoes().stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        NfeImportacaoDto.Associacao::numeroItem))
                        .toList();
        String hash =
                operacoes.hash(
                        OrigemEntradaService.RECURSO,
                        List.of(dados.clienteId(), dados.armazemId()),
                        List.of(
                                dados.referencia(),
                                dados.xml(),
                                dados.revisaoPrevia(),
                                associacoes));
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, NfeImportacaoDto.Confirmacao.class);
        if (repetida != null) {
            pedidos.obter(repetida.pedido().id());
            return repetida;
        }
        var previa =
                analisar(
                        new NfeImportacaoDto.Ler(
                                dados.xml(), dados.clienteId(), dados.armazemId(), associacoes),
                        true);
        if (!previa.revisaoPrevia().equals(dados.revisaoPrevia()))
            throw RegraNegocioException.conflito(
                    "PREVIA_DESATUALIZADA",
                    "A prévia mudou. Atualize e confira antes de confirmar.");
        if (!previa.podeConfirmar())
            throw CadastroSupport.invalido(String.join(" ", previa.pendencias()));
        var criado =
                pedidos.criar(
                        new PedidoEntradaDto.Criar(
                                dados.clienteId(), dados.armazemId(), dados.referencia()));
        var doc = previa.documento();
        var porNumero = new HashMap<Integer, NfeImportacaoDto.ItemAssociado>();
        previa.itens().forEach(i -> porNumero.put(i.numeroItem(), i));
        var itens =
                doc.itens().stream()
                        .map(
                                i ->
                                        new PedidoEntradaDto.ItemNota(
                                                i.numeroItem(),
                                                porNumero.get(i.numeroItem()).produtoId(),
                                                porNumero.get(i.numeroItem()).quantidadeEstoque(),
                                                i.valorProduto()))
                        .toList();
        inclusao.adicionarXmlAssociado(
                criado.id(),
                new PedidoEntradaDto.NotaManual(
                        criado.versao(),
                        doc.serie(),
                        doc.numero(),
                        OffsetDateTime.parse(doc.emitidaEm()).toLocalDate(),
                        doc.chaveAcesso(),
                        itens),
                dados.xml());
        var pedido = pedidos.obter(criado.id());
        var resposta =
                new NfeImportacaoDto.Confirmacao(
                        OrigemEntradaService.RECURSO,
                        dados.operacaoId(),
                        previa.xmlHash(),
                        PedidoEntradaDto.Resumo.de(pedido, "XML"));
        operacoes.salvar(
                dados.operacaoId(), "CRIACAO", cliente, armazem, pedido.getId(), hash, resposta);
        return resposta;
    }

    public NfeImportacaoDto.Confirmacao resultado(@NotNull UUID operacaoId) {
        acesso.usuario();
        var op =
                recibos.findByOperacaoId(operacaoId.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (!OrigemEntradaService.RECURSO.equals(
                mapper.readTree(op.getResultado()).path("tipoRecurso").asString()))
            throw RegraNegocioException.naoEncontrado();
        acesso.cliente(op.getCliente().getId());
        acesso.armazem(op.getArmazem().getId());
        var resposta = mapper.readValue(op.getResultado(), NfeImportacaoDto.Confirmacao.class);
        pedidos.obter(resposta.pedido().id());
        return resposta;
    }

    public NfeImportacaoDto.DocumentoSalvo documento(
            @NotNull @Positive Long pedidoId, @NotNull @Positive Long notaId) {
        pedidos.obter(pedidoId);
        var nota = notas.findById(notaId).orElseThrow(RegraNegocioException::naoEncontrado);
        if (!nota.getPedido().getId().equals(pedidoId) || nota.getXmlOriginal() == null)
            throw RegraNegocioException.naoEncontrado();
        return new NfeImportacaoDto.DocumentoSalvo(
                documentos.ler(nota.getXmlOriginal()), nota.getXmlOriginal(), nota.getXmlHash());
    }
}
