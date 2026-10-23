package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.NotazzProperties;
import br.com.rodogarcia.wms.dto.FiscalNfeDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CoberturaDocumentoSaida;
import br.com.rodogarcia.wms.models.DocumentoSaida;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SolicitacaoNfe;
import br.com.rodogarcia.wms.repositories.CoberturaDocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FiscalNfeRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalNfeTransacaoService {
    private final FiscalNfeRepository repository;
    private final PedidoSaidaService pedidos;
    private final PedidoSaidaRepository pedidoRepository;
    private final FiscalNfePreparacaoService preparacao;
    private final DocumentoSaidaRepository documentos;
    private final CoberturaDocumentoSaidaRepository coberturas;
    private final ReservaSaidaRepository reservas;
    private final FiscalNfeXml xml;
    private final AcessoService acesso;
    private final NotazzProperties properties;
    private final JsonMapper mapper;
    private final Clock clock;
    private final AuditoriaService auditoria;

    public FiscalNfeTransacaoService(
            FiscalNfeRepository repository,
            PedidoSaidaService pedidos,
            PedidoSaidaRepository pedidoRepository,
            FiscalNfePreparacaoService preparacao,
            DocumentoSaidaRepository documentos,
            CoberturaDocumentoSaidaRepository coberturas,
            ReservaSaidaRepository reservas,
            FiscalNfeXml xml,
            AcessoService acesso,
            NotazzProperties properties,
            JsonMapper mapper,
            Clock clock,
            AuditoriaService auditoria) {
        this.repository = repository;
        this.pedidos = pedidos;
        this.pedidoRepository = pedidoRepository;
        this.preparacao = preparacao;
        this.documentos = documentos;
        this.coberturas = coberturas;
        this.reservas = reservas;
        this.xml = xml;
        this.acesso = acesso;
        this.properties = properties;
        this.mapper = mapper;
        this.clock = clock;
        this.auditoria = auditoria;
    }

    @Transactional
    public List<SolicitacaoNfe> confirmar(Long pedidoId, FiscalNfeDto.Confirmar comando) {
        acesso.exigirSupervisor();
        var pedido = pedidos.bloquear(pedidoId);
        String hashComando = NfeXmlService.hash(mapper.writeValueAsString(comando));
        var repetidas = repository.daOperacao(comando.operacaoId().toString());
        if (!repetidas.isEmpty()) {
            if (repetidas.stream()
                    .anyMatch(
                            s ->
                                    s.pedidoId() != pedidoId
                                            || !s.hashConfirmacao().equals(hashComando)))
                throw conflito(
                        "OPERACAO_DIVERGENTE",
                        "Esta confirmação já foi utilizada com outros dados.");
            return repetidas;
        }
        var preparadas =
                preparacao.preparar(
                        pedidoId,
                        new FiscalNfeDto.Preparar(
                                comando.versao(), comando.natureza(), comando.documentoOrigemId()));
        var previa = preparadas.previa();
        if (!previa.podeEmitir())
            throw CadastroSupport.invalido(String.join(" ", previa.pendencias()));
        if (!previa.hash().equals(comando.hash()))
            throw conflito(
                    "PREVIA_DESATUALIZADA",
                    "Os dados fiscais mudaram. Confira novamente antes de emitir.");
        var ativas = repository.ativas(pedidoId, comando.natureza());
        for (var d : previa.documentos())
            if (ativas.stream().anyMatch(s -> s.grupo().equals(d.grupo())))
                throw conflito(
                        "EMISSAO_EXISTENTE",
                        "Já existe uma solicitação para esta operação. Consulte seu resultado antes de emitir outra nota.");
        Instant agora = agora();
        String plano = UUID.randomUUID().toString();
        for (int index = 0; index < previa.documentos().size(); index++) {
            var doc = previa.documentos().get(index);
            String id = UUID.randomUUID().toString();
            var s =
                    new SolicitacaoNfe(
                            id,
                            plano,
                            comando.operacaoId().toString(),
                            pedidoId,
                            doc.grupo(),
                            doc.natureza(),
                            pedido.getVersao(),
                            hashComando,
                            preparadas.corpos().get(index),
                            mapper.writeValueAsString(doc),
                            doc.emitenteCnpj(),
                            doc.ambiente(),
                            "PREPARADA",
                            null,
                            null,
                            doc.documentoOrigemId(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Solicitação gravada. Aguardando envio.",
                            acesso.usuario(),
                            agora,
                            agora);
            repository.inserir(s);
            repository.evento(
                    id,
                    "CONFIRMADA",
                    acesso.usuario(),
                    CadastroSupport.motivo(comando.motivo()),
                    agora);
            for (var item : doc.itens())
                repository.parcela(
                        id,
                        item.reservaId(),
                        item.entradaOrigemId(),
                        item.itemNotaOrigemId(),
                        item.quantidade(),
                        item.valorTotal());
        }
        return repository.daOperacao(comando.operacaoId().toString());
    }

    public SolicitacaoNfe obter(Long pedido, String id) {
        var scope =
                pedidoRepository
                        .buscarEscopo(pedido)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(scope.getClienteId());
        acesso.armazem(scope.getArmazemId());
        var s = repository.consultar(id, false);
        if (s == null || s.pedidoId() != pedido) throw RegraNegocioException.naoEncontrado();
        return s;
    }

    @Transactional
    public SolicitacaoNfe adquirir(
            Long pedidoId, String id, String acao, FiscalNfeDto.Comando comando, String dono) {
        acesso.exigirSupervisor();
        var pedido = pedidos.bloquear(pedidoId);
        var s = obter(pedidoId, id);
        s = repository.consultar(id, true);
        if (s.situacao().equals("CANCELADA")) return null;
        if (acao.equals("CRIAR") && !s.situacao().equals("PREPARADA")) return null;
        var emitente = properties.getEmitentes().get(pedido.getArmazem().getId());
        if (!properties.isEnabled()
                || emitente == null
                || !emitente.isConfiguracaoConferida()
                || emitente.getApiKey() == null
                || emitente.getApiKey().isBlank()
                || !properties.ambientePermitido(emitente))
            throw CadastroSupport.invalido(
                    "Integração fiscal não está habilitada/conferida para este ambiente.");
        if (emitente == null
                || !Objects.equals(s.emitenteCnpj(), emitente.getCnpj())
                || s.ambiente() != emitente.getAmbiente())
            throw conflito(
                    "EMITENTE_DIVERGENTE",
                    "Configuração atual não corresponde ao estabelecimento/ambiente da solicitação.");
        if (acao.equals("CRIAR")) {
            if (!s.natureza().equals("REGULARIZACAO_ENTRADA")
                    && pedido.getSituacao() != SituacaoPedidoSaida.SEPARADO)
                throw conflito(
                        "PEDIDO_ALTERADO",
                        "Pedido deixou de permitir nova emissão. Preserve a solicitação e resolva a ocorrência.");
            // Revalida disponibilidade atual sem reconstruir ou substituir o snapshot confirmado.
            preparacao.validarEnvio(s, pedido);
        }
        if (acao.equals("CANCELAR")) {
            Long documentoSaida = s.documentoId();
            if (documentoSaida != null
                    && repository.doPedido(pedidoId).stream()
                            .anyMatch(
                                    e ->
                                            e.natureza().equals("REGULARIZACAO_ENTRADA")
                                                    && Objects.equals(
                                                            e.documentoOrigemId(), documentoSaida)
                                                    && !e.situacao().equals("CANCELADA")))
                throw conflito(
                        "REGULARIZACAO_EXISTENTE",
                        "Esta saída já tem uma nova entrada em andamento. Conclua sua conciliação antes de solicitar cancelamento.");
            var eventos = repository.eventosComando(id, comando.operacaoId().toString());
            if (!eventos.isEmpty()) {
                if (!eventos.getFirst().equals(comando.motivo()))
                    throw conflito(
                            "OPERACAO_DIVERGENTE",
                            "Motivo alterado para a mesma operação de cancelamento.");
                return null;
            }
            if (!s.situacao().equals("AUTORIZADA") || s.natureza().equals("REGULARIZACAO_ENTRADA"))
                throw conflito(
                        "CANCELAMENTO_INDISPONIVEL",
                        "Documento não permite solicitação de cancelamento.");
            var autorizacao =
                    xml.conferir(
                            s.xml(),
                            mapper.readValue(
                                    s.documentoJson(), FiscalNfeDto.DocumentoPlanejado.class));
            if (pedido.getSituacao() == SituacaoPedidoSaida.RETIRADO
                    || Duration.between(autorizacao.autorizadaEm(), agora())
                                    .compareTo(Duration.ofHours(24))
                            > 0)
                throw conflito(
                        "NOVA_ENTRADA_NECESSARIA",
                        "Houve retirada ou o prazo de cancelamento foi ultrapassado. Emita nova nota de entrada para regularizar o retorno.");
        }
        Instant agora = agora();
        if (!repository.ocuparCanal(
                s.emitenteCnpj(), s.ambiente(), dono, agora, agora.plusSeconds(45))) return null;
        if (acao.equals("CRIAR"))
            repository.estado(
                    id,
                    "ENVIANDO",
                    "Envio iniciado. Resultado será conciliado pela mesma identidade.",
                    agora);
        if (acao.equals("CANCELAR")) {
            repository.evento(
                    id,
                    "CANCELAR:" + comando.operacaoId(),
                    acesso.usuario(),
                    comando.motivo(),
                    agora);
            repository.estado(
                    id,
                    "CANCELANDO",
                    "Cancelamento solicitado; aguardando confirmação fiscal.",
                    agora);
        }
        repository.evento(
                id,
                acao,
                acesso.usuario(),
                "Operação fiscal iniciada; sem movimentação de estoque.",
                agora);
        return s;
    }

    @Transactional
    public void recebido(Long pedidoId, String id, String acao, JsonNode retorno, String dono) {
        acesso.exigirSupervisor();
        var pedido = pedidos.bloquear(pedidoId);
        var s = repository.consultar(id, true);
        if (s == null || s.pedidoId() != pedidoId) throw RegraNegocioException.naoEncontrado();
        String remoto = valor(retorno, "id");
        String external = valor(retorno, "external_id");
        if ((external != null && !external.equals(id))
                || s.notazzId() != null && remoto != null && !s.notazzId().equals(remoto)) {
            estado(s, "CONFLITO", "Identidade do retorno fiscal divergente.", dono);
            return;
        }
        String codigo = valor(retorno, "codigoProcessamento");
        String status = valor(retorno, "statusNota");
        if (!"000".equals(codigo)
                || !"sucesso".equalsIgnoreCase(valor(retorno, "statusProcessamento"))) {
            estado(
                    s,
                    Set.of("AUTORIZADA", "CANCELADA", "CANCELANDO").contains(s.situacao())
                            ? s.situacao()
                            : "DESCONHECIDO",
                    "O emissor não confirmou o resultado. Consulte novamente antes de qualquer reenvio.",
                    dono);
            return;
        }
        if (!acao.equals("CRIAR")
                && !acao.equals("CANCELAR")
                && (remoto == null || s.notazzId() == null && !id.equals(external))) {
            estado(
                    s,
                    "CONFLITO",
                    "Consulta sem vínculo externo verificável com esta solicitação.",
                    dono);
            return;
        }
        if (acao.equals("CRIAR")) {
            if (remoto == null || remoto.length() > 100) {
                estado(
                        s,
                        "DESCONHECIDO",
                        "Criação sem identificador fiscal válido. Consulte a mesma solicitação.",
                        dono);
                return;
            }
            repository.resultado(
                    id,
                    "PROCESSANDO",
                    remoto,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Recebida pela Notazz; autorização ainda não confirmada.",
                    agora());
        } else if ("Autorizada".equalsIgnoreCase(status)) {
            if (s.situacao().equals("CANCELADA")) {
                estado(
                        s,
                        "CANCELADA",
                        "Evento autorizado antigo ignorado; cancelamento confirmado foi preservado.",
                        dono);
                return;
            }
            if (s.situacao().equals("CANCELANDO")) {
                estado(
                        s,
                        "CANCELANDO",
                        "A nota permanece autorizada e o cancelamento ainda não foi confirmado. Consulte o resultado; não emita uma regularização em paralelo.",
                        dono);
                return;
            }
            try {
                String conteudo = decodificar(retorno, "xmlBase64");
                var snapshot =
                        mapper.readValue(s.documentoJson(), FiscalNfeDto.DocumentoPlanejado.class);
                var auth = xml.conferir(conteudo, snapshot);
                var n = auth.nota();
                String numeroRetornado = valor(retorno, "numero");
                if (numeroRetornado == null
                        || !numeroRetornado.matches("[0-9]{1,9}")
                        || !n.numero().equals(Long.toString(Long.parseLong(numeroRetornado)))
                        || !Objects.equals(n.chaveAcesso(), valor(retorno, "chave")))
                    throw CadastroSupport.invalido("Identidade fiscal divergente.");
                Long documento = s.documentoId();
                if (documento == null && !s.natureza().equals("REGULARIZACAO_ENTRADA")) {
                    var existente =
                            documentos.findByPedidoIdOrderById(pedidoId).stream()
                                    .filter(
                                            d ->
                                                    Objects.equals(
                                                            d.getChaveAcesso(), n.chaveAcesso()))
                                    .findFirst()
                                    .orElse(null);
                    if (existente != null)
                        throw conflito(
                                "IDENTIDADE_EXISTENTE",
                                "Documento já registrado; conciliação manual necessária para preservar cobertura.");
                    var d =
                            documentos.saveAndFlush(
                                    new DocumentoSaida(
                                            pedido,
                                            "NOTAZZ",
                                            s.natureza(),
                                            n.emitenteCnpj(),
                                            n.serie(),
                                            n.numero(),
                                            n.emissao(),
                                            n.chaveAcesso(),
                                            auth.protocolo(),
                                            NfeXmlService.hash(conteudo),
                                            agora()));
                    for (var c : snapshot.coberturas()) {
                        var r =
                                reservas.findById(c.reservaId())
                                        .orElseThrow(RegraNegocioException::naoEncontrado);
                        if (!r.getItem().getPedido().getId().equals(pedidoId))
                            throw conflito("ORIGEM_DIVERGENTE", "Cobertura fora do pedido.");
                        var anterior =
                                coberturas.buscarDoPedido(pedidoId).stream()
                                        .filter(
                                                a ->
                                                        a.getReserva().getId().equals(r.getId())
                                                                && "AUTORIZADO"
                                                                        .equals(
                                                                                a.getDocumento()
                                                                                        .getSituacao())
                                                                && a.getDocumento()
                                                                        .getNatureza()
                                                                        .equals(s.natureza()))
                                        .map(CoberturaDocumentoSaida::getQuantidade)
                                        .reduce(
                                                java.math.BigDecimal.ZERO,
                                                java.math.BigDecimal::add);
                        if (anterior.add(c.quantidade()).compareTo(r.getQuantidade()) > 0)
                            throw conflito(
                                    "COBERTURA_EXCEDIDA",
                                    "Cobertura já existente deve ser conciliada.");
                        coberturas.save(new CoberturaDocumentoSaida(d, r, c.quantidade()));
                    }
                    documento = d.getId();
                    pedido.atualizar(pedido.getSituacao(), agora());
                    auditoria.registrar(
                            "PEDIDO_SAIDA",
                            pedidoId,
                            "DOCUMENTO_SAIDA",
                            "NF-e autorizada pela integração e conciliada com o pedido.",
                            null,
                            java.util.Map.of(
                                    "solicitacao",
                                    id,
                                    "numero",
                                    n.numero(),
                                    "chave",
                                    n.chaveAcesso()));
                }
                repository.resultado(
                        id,
                        "AUTORIZADA",
                        remoto,
                        n.numero(),
                        n.serie(),
                        n.chaveAcesso(),
                        auth.protocolo(),
                        conteudo,
                        documento,
                        "NF-e autorizada e conciliada. A movimentação física continua em etapa própria.",
                        agora());
            } catch (RegraNegocioException | IllegalArgumentException ex) {
                // Uma inconsistência XML deve ser detectada antes de qualquer escrita de cobertura.
                throw ex;
            }
        } else if ("Cancelada".equalsIgnoreCase(status)) {
            xml.conferirCancelamento(
                    decodificar(retorno, "xmlCancelamentoBase64"),
                    s.chaveAcesso(),
                    s.emitenteCnpj(),
                    s.ambiente());
            if (!s.situacao().equals("CANCELADA"))
                auditoria.registrar(
                        "PEDIDO_SAIDA",
                        pedidoId,
                        "CANCELAMENTO_DOCUMENTO",
                        "Cancelamento fiscal confirmado pela integração.",
                        java.util.Map.of("solicitacao", id, "situacao", s.situacao()),
                        java.util.Map.of("situacao", "CANCELADA"));
            if (s.documentoId() != null)
                documentos
                        .findById(s.documentoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado)
                        .cancelar(agora());
            repository.resultado(
                    id,
                    "CANCELADA",
                    remoto,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Cancelamento fiscal confirmado. Retorno físico exige confirmação própria.",
                    agora());
            pedido.atualizar(pedido.getSituacao(), agora());
        } else if ("EmProcessoDeCancelamento".equalsIgnoreCase(status)) {
            if (!s.situacao().equals("CANCELADA"))
                repository.estado(
                        id,
                        "CANCELANDO",
                        "Cancelamento em processamento no emissor. Aguarde sua confirmação antes da regularização.",
                        agora());
        } else if ("EmConflito".equalsIgnoreCase(status)) {
            if (!s.situacao().equals("CANCELADA"))
                repository.estado(
                        id,
                        "CONFLITO",
                        "O emissor informou conflito. Confira a solicitação com o fiscal antes de prosseguir.",
                        agora());
        } else if ("Rejeitada".equalsIgnoreCase(status) || "Denegada".equalsIgnoreCase(status)) {
            if (s.situacao().equals("AUTORIZADA") || s.situacao().equals("CANCELADA")) {
                estado(
                        s,
                        s.situacao(),
                        "Retorno anterior ignorado. Situação fiscal confirmada preservada.",
                        dono);
                return;
            }
            repository.resultado(
                    id,
                    "REJEITADA",
                    remoto,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Documento rejeitado pelo emissor. Corrija os dados no fiscal e consulte esta solicitação; não crie emissão duplicada.",
                    agora());
        } else if (!Set.of("AUTORIZADA", "CANCELADA", "CANCELANDO").contains(s.situacao()))
            repository.resultado(
                    id,
                    "PROCESSANDO",
                    remoto,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Processamento fiscal em andamento. Consulte novamente.",
                    agora());
        repository.evento(
                id,
                "RETORNO_" + acao,
                acesso.usuario(),
                "Resposta fiscal consultada e conciliada com a solicitação.",
                agora());
        repository.liberarCanal(s.emitenteCnpj(), s.ambiente(), dono, agora().plusMillis(650));
    }

    @Transactional
    public void falhou(Long pedidoId, String id, String dono, boolean conflito) {
        acesso.exigirSupervisor();
        pedidos.bloquear(pedidoId);
        var s = obter(pedidoId, id);
        s = repository.consultar(id, true);
        String estado =
                Set.of("CANCELADA", "CANCELANDO").contains(s.situacao())
                        ? s.situacao()
                        : conflito
                                ? "CONFLITO"
                                : s.situacao().equals("AUTORIZADA") ? "AUTORIZADA" : "DESCONHECIDO";
        estado(
                s,
                estado,
                conflito
                        ? "Retorno fiscal divergente. A solicitação foi preservada para conciliação."
                        : "Resultado ainda desconhecido. Consulte a mesma solicitação; não emita outra nota.",
                dono);
    }

    private void estado(SolicitacaoNfe s, String estado, String mensagem, String dono) {
        repository.estado(s.id(), estado, mensagem, agora());
        repository.evento(s.id(), estado, acesso.usuario(), mensagem, agora());
        repository.liberarCanal(s.emitenteCnpj(), s.ambiente(), dono, agora().plusMillis(650));
    }

    private Instant agora() {
        return clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    private static String valor(JsonNode x, String key) {
        var n = x.path(key);
        return n.isMissingNode() || n.isNull() ? null : n.asString();
    }

    private static String decodificar(JsonNode x, String key) {
        String b = valor(x, key);
        if (b == null || b.length() > 1_400_000)
            throw CadastroSupport.invalido("XML autorizado indisponível ou excede o limite.");
        byte[] bytes = Base64.getDecoder().decode(b);
        if (bytes.length > 1_000_000) throw CadastroSupport.invalido("XML excede o limite.");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static RegraNegocioException conflito(String c, String m) {
        return RegraNegocioException.conflito(c, m);
    }
}
