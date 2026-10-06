package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.NfeEntradaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.BaixaSaida;
import br.com.rodogarcia.wms.models.CoberturaDocumentoSaida;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.DocumentoSaida;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.OperacaoSaida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.RetiradaSaida;
import br.com.rodogarcia.wms.models.SeparacaoSaida;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.CoberturaDocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.DevolucaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.DocumentoSaidaRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.OperacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import br.com.rodogarcia.wms.repositories.RetiradaSaidaRepository;
import br.com.rodogarcia.wms.repositories.SeparacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ExpedicaoService {
    private final PedidoSaidaService pedidos;
    private final PedidoSaidaRepository pedidoRepository;
    private final ReservaSaidaRepository reservas;
    private final SeparacaoSaidaRepository separacoes;
    private final DocumentoSaidaRepository documentos;
    private final CoberturaDocumentoSaidaRepository coberturas;
    private final RetiradaSaidaRepository retiradas;
    private final BaixaSaidaRepository baixas;
    private final DevolucaoSaidaRepository devolucoes;
    private final FatoPermanenciaRepository fatos;
    private final ConteudoUnidadeRepository conteudos;
    private final UnidadeLogisticaRepository unidades;
    private final ProdutoRepository produtos;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final OperacaoSaidaRepository operacoes;
    private final MovimentoSaidaEstoqueService movimento;
    private final RegistroDevolucaoService retornos;
    private final ResolucaoCadastroService resolucao;
    private final EstoqueService estoque;
    private final NfeXmlService xml;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final JsonMapper mapper;
    private final Clock clock;
    private final HistoricoOperacaoTemporalService temporal;

    public ExpedicaoService(
            PedidoSaidaService pedidos,
            PedidoSaidaRepository pedidoRepository,
            ReservaSaidaRepository reservas,
            SeparacaoSaidaRepository separacoes,
            DocumentoSaidaRepository documentos,
            CoberturaDocumentoSaidaRepository coberturas,
            RetiradaSaidaRepository retiradas,
            BaixaSaidaRepository baixas,
            DevolucaoSaidaRepository devolucoes,
            FatoPermanenciaRepository fatos,
            ConteudoUnidadeRepository conteudos,
            UnidadeLogisticaRepository unidades,
            ProdutoRepository produtos,
            OcupacaoEnderecoRepository ocupacoes,
            OperacaoSaidaRepository operacoes,
            MovimentoSaidaEstoqueService movimento,
            RegistroDevolucaoService retornos,
            ResolucaoCadastroService resolucao,
            EstoqueService estoque,
            NfeXmlService xml,
            AcessoService acesso,
            AuditoriaService auditoria,
            JsonMapper mapper,
            Clock clock,
            HistoricoOperacaoTemporalService temporal) {
        this.pedidos = pedidos;
        this.pedidoRepository = pedidoRepository;
        this.reservas = reservas;
        this.separacoes = separacoes;
        this.documentos = documentos;
        this.coberturas = coberturas;
        this.retiradas = retiradas;
        this.baixas = baixas;
        this.devolucoes = devolucoes;
        this.fatos = fatos;
        this.conteudos = conteudos;
        this.unidades = unidades;
        this.produtos = produtos;
        this.ocupacoes = ocupacoes;
        this.operacoes = operacoes;
        this.movimento = movimento;
        this.retornos = retornos;
        this.resolucao = resolucao;
        this.estoque = estoque;
        this.xml = xml;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
        this.temporal = temporal;
    }

    public ExpedicaoDto.Detalhe consultar(@NotNull @Positive Long id) {
        var p = pedidoRepository.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(p);
        return detalhe(p);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao ler(
            @NotNull @Positive Long id, @NotNull @Valid ExpedicaoDto.Leitura d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        var p = pedidos.bloquear(id);
        String hash = hash("LEITURA_SAIDA", d);
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        conferir(
                p,
                d.versao(),
                d.resolverPendentes(),
                SituacaoPedidoSaida.RESERVADO,
                SituacaoPedidoSaida.EM_SEPARACAO);
        var r = reserva(p, d.reservaId());
        var u = r.getUnidade();
        validarFisico(p, r, TipoEndereco.ARMAZENAGEM, d.resolverPendentes());
        if (!u.getCodigo().equals(d.codigoLido().toString())
                || u.getRevisaoConteudo() != d.revisaoConteudo())
            throw conflito("ETIQUETA_INVALIDA", "Leia a etiqueta vigente da unidade reservada.");
        var antes = detalhe(p);
        var s = separacoes.findByReservaId(r.getId()).orElse(null);
        if (s != null && !s.getSituacao().equals("LEITURA"))
            throw conflito("SITUACAO_INVALIDA", "Reserva já separada.");
        if (s == null) separacoes.save(new SeparacaoSaida(r, agora()));
        else s.ler(agora());
        p.atualizar(p.getSituacao(), agora());
        return concluir(p, antes, d.operacaoId(), hash, "LEITURA_SAIDA", d.motivo(), null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao separar(
            @NotNull @Positive Long id, @NotNull @Valid ExpedicaoDto.Separar d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        var p = pedidos.bloquear(id);
        String hash = hash("SEPARACAO_SAIDA", d);
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        conferir(
                p,
                d.versao(),
                d.resolverPendentes(),
                SituacaoPedidoSaida.RESERVADO,
                SituacaoPedidoSaida.EM_SEPARACAO);
        var r = reserva(p, d.destinacao().reservaId());
        var u = r.getUnidade();
        validarFisico(p, r, TipoEndereco.ARMAZENAGEM, d.resolverPendentes());
        var s =
                separacoes
                        .findByReservaId(r.getId())
                        .orElseThrow(
                                () ->
                                        conflito(
                                                "LEITURA_NECESSARIA",
                                                "Leia a unidade antes da separação."));
        if (!s.getSituacao().equals("LEITURA")
                || s.getRevisaoConteudoLida() != u.getRevisaoConteudo()
                || s.getVersaoUnidadeLida() != u.getVersao())
            throw conflito(
                    "LEITURA_DESATUALIZADA", "Unidade alterada após leitura; leia novamente.");
        var antes = detalhe(p);
        var origens =
                ocupacoes.buscarDasUnidades(List.of(u.getId())).stream()
                        .map(
                                o ->
                                        new EstoqueDto.Destino(
                                                o.getEndereco().getId(),
                                                o.getEndereco().getCodigo()))
                        .toList();
        var conjunto = u.getConjuntoAtual();
        movimento.mover(
                u,
                d.destinacao().destinos(),
                d.destinacao().conjuntoId(),
                TipoEndereco.SEPARACAO,
                d.resolverPendentes(),
                agora());
        s.separar(mapper.writeValueAsString(origens), conjunto, agora());
        registrarFato(
                u, d.operacaoId(), "SEPARACAO", u.getQuantidade(), u.getQuantidade(), agora());
        separacoes.flush();
        boolean todas =
                ativas(p).stream()
                        .allMatch(
                                a ->
                                        separacoes
                                                .findByReservaId(a.getId())
                                                .map(b -> b.getSituacao().equals("SEPARADA"))
                                                .orElse(false));
        p.atualizar(
                todas ? SituacaoPedidoSaida.SEPARADO : SituacaoPedidoSaida.EM_SEPARACAO, agora());
        return concluir(p, antes, d.operacaoId(), hash, "SEPARACAO_SAIDA", d.motivo(), null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao registrarDocumento(
            @NotNull @Positive Long id, @NotNull @Valid ExpedicaoDto.Documento d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        String hash = hash("DOCUMENTO_SAIDA", d);
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        conferir(p, d.versao(), d.resolverPendentes(), SituacaoPedidoSaida.SEPARADO);
        ExpedicaoDto.Nota n = d.nota();
        NfeEntradaDto extraido = null;
        if (d.origem().equals("XML")) {
            if (d.xml() == null) throw CadastroSupport.invalido("Informe XML existente.");
            extraido = xml.ler(d.xml());
            n = nota(extraido);
            if (d.nota() != null && !normalizar(d.nota()).equals(n))
                throw CadastroSupport.invalido("Dados não correspondem ao XML.");
        } else {
            if (n == null || d.xml() != null)
                throw CadastroSupport.invalido(
                        "NOTAZZ exige identidade do documento existente; XML usa origem XML.");
            n = normalizar(n);
        }
        if (n.emissao().isAfter(LocalDate.now(clock)))
            throw CadastroSupport.invalido("Emissão futura.");
        var novas = new HashMap<Long, BigDecimal>();
        var porSku = new HashMap<String, BigDecimal>();
        for (var c : d.coberturas()) {
            var r = reserva(p, c.reservaId());
            var u = r.getUnidade();
            if (novas.putIfAbsent(r.getId(), c.quantidade()) != null
                    || !u.getNota().getId().equals(c.notaOrigemId())
                    || !u.getProduto().getSku().equals(CadastroSupport.codigo(c.sku())))
                throw CadastroSupport.invalido("Cobertura repetida ou origem/SKU divergente.");
            CadastroSupport.quantidade(u.getProduto(), c.quantidade());
            porSku.merge(u.getProduto().getSku(), c.quantidade(), BigDecimal::add);
            BigDecimal anterior = coberturaAtiva(p, r.getId());
            if (d.natureza().equals("RETORNO_MERCADORIA")
                    && anterior.add(c.quantidade()).compareTo(r.getQuantidade()) > 0)
                throw conflito("COBERTURA_EXCEDIDA", "Documentos excedem quantidade reservada.");
        }
        if (extraido != null) conferirXmlItens(extraido, porSku, p);
        var antes = detalhe(p);
        var doc =
                documentos.save(
                        new DocumentoSaida(
                                p,
                                d.origem(),
                                d.natureza(),
                                n.emitenteCnpj(),
                                n.serie(),
                                n.numero(),
                                n.emissao(),
                                n.chaveAcesso(),
                                CadastroSupport.texto(d.protocolo()),
                                d.xml() == null ? null : NfeXmlService.hash(d.xml()),
                                agora()));
        for (var c : d.coberturas())
            coberturas.save(
                    new CoberturaDocumentoSaida(doc, reserva(p, c.reservaId()), c.quantidade()));
        p.atualizar(p.getSituacao(), agora());
        return concluir(p, antes, d.operacaoId(), hash, "DOCUMENTO_SAIDA", d.motivo(), null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao cancelarDocumento(
            @NotNull @Positive Long id,
            @NotNull @Positive Long documentoId,
            @NotNull @Valid ExpedicaoDto.CancelarDocumento d) {
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        String hash = hash("CANCELAMENTO_DOCUMENTO", List.of(documentoId, d));
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        versao(p, d.versao());
        var doc =
                documentos.findById(documentoId).orElseThrow(RegraNegocioException::naoEncontrado);
        if (!doc.getPedido().getId().equals(id)) throw RegraNegocioException.naoEncontrado();
        if (!doc.getSituacao().equals("AUTORIZADO"))
            throw conflito("SITUACAO_INVALIDA", "Documento já cancelado.");
        var antes = detalhe(p);
        doc.cancelar(agora());
        p.atualizar(p.getSituacao(), agora());
        return concluir(p, antes, d.operacaoId(), hash, "CANCELAMENTO_DOCUMENTO", d.motivo(), null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao retirar(
            @NotNull @Positive Long id, @NotNull @Valid ExpedicaoDto.Retirar d) {
        return retirarDatado(id, d, null, null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao retirarContingencia(
            @NotNull @Positive Long id,
            @NotNull @Valid ExpedicaoDto.Retirar d,
            @NotNull Instant ocorridaEm,
            @NotNull String identidadeFato) {
        acesso.exigirSupervisor();
        return retirarDatado(id, d, ocorridaEm, identidadeFato);
    }

    private ExpedicaoDto.Confirmacao retirarDatado(
            Long id, ExpedicaoDto.Retirar d, Instant ocorridaEm, String identidadeFato) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        String hash =
                hash(
                        "RETIRADA_FISICA",
                        ocorridaEm == null ? d : List.of(d, ocorridaEm, identidadeFato));
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        conferir(p, d.versao(), d.resolverPendentes(), SituacaoPedidoSaida.SEPARADO);
        var linhas = ativas(p);
        validarIntegral(p, linhas);
        for (var r : linhas) {
            validarFisico(p, r, TipoEndereco.SEPARACAO, d.resolverPendentes());
            var s =
                    separacoes
                            .findByReservaId(r.getId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!s.getSituacao().equals("SEPARADA")
                    || s.getRevisaoConteudoLida() != r.getUnidade().getRevisaoConteudo())
                throw conflito(
                        "LEITURA_DESATUALIZADA",
                        "Leitura/separação não corresponde ao conteúdo vigente.");
            if (coberturaAtiva(p, r.getId()).compareTo(r.getQuantidade()) != 0)
                throw conflito(
                        "DOCUMENTACAO_INCOMPLETA",
                        "Documentos autorizados precisam cobrir integralmente todas as reservas.");
            if (ocorridaEm != null) {
                temporal.conferir(r.getUnidade(), ocorridaEm);
                if (s.getSeparadaEm() == null
                        || s.getSeparadaEm().isAfter(ocorridaEm)
                        || s.getLidaEm().isAfter(ocorridaEm)
                        || r.getCriadaEm().isAfter(ocorridaEm))
                    throw conflito(
                            "HISTORICO_CONTINGENCIA_INSUFICIENTE",
                            "Reserva/leitura/separação precisam anteceder a retirada comprovada.");
            }
        }
        conferirComprovantes(p, d.xmls());
        var destinos = destinos(d.remanescentes());
        var parciais =
                linhas.stream()
                        .filter(
                                r ->
                                        r.getQuantidade().compareTo(r.getUnidade().getQuantidade())
                                                < 0)
                        .map(ReservaSaida::getId)
                        .collect(Collectors.toSet());
        if (!destinos.keySet().equals(parciais))
            throw CadastroSupport.invalido(
                    "Informe destino de todos e somente os pallets remanescentes.");
        var antes = detalhe(p);
        var instante = ocorridaEm == null ? agora() : ocorridaEm.truncatedTo(ChronoUnit.MICROS);
        var retirada =
                retiradas.save(
                        new RetiradaSaida(
                                p,
                                instante,
                                acesso.usuario(),
                                NfeXmlService.hash(mapper.writeValueAsString(d.xmls()))));
        for (var r : linhas) {
            var u = r.getUnidade();
            var quantidadeAntes = u.getQuantidade();
            BigDecimal restante = r.getQuantidade();
            var origens = conteudos.buscarOrigens(u.getId());
            if (origens.stream()
                            .map(ConteudoUnidade::getQuantidade)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .compareTo(quantidadeAntes)
                    != 0)
                throw conflito("COMPOSICAO_INCONSISTENTE", "Conteúdo não conserva origens.");
            for (var c : origens) {
                if (restante.signum() == 0) break;
                BigDecimal baixa = c.getQuantidade().min(restante);
                if (baixa.signum() > 0) {
                    baixas.save(new BaixaSaida(retirada, r, c.getEntrada(), baixa));
                    c.alterarQuantidade(c.getQuantidade().subtract(baixa));
                    restante = restante.subtract(baixa);
                }
            }
            if (restante.signum() != 0)
                throw conflito("COMPOSICAO_INCONSISTENTE", "Origem insuficiente para baixa.");
            BigDecimal depois = quantidadeAntes.subtract(r.getQuantidade());
            registrarFato(u, d.operacaoId(), "RETIRADA", quantidadeAntes, depois, instante);
            u.alterarQuantidade(depois, instante);
            if (depois.signum() > 0) {
                var dest = destinos.get(r.getId());
                movimento.mover(
                        u,
                        dest.destinos(),
                        dest.conjuntoId(),
                        TipoEndereco.ARMAZENAGEM,
                        d.resolverPendentes(),
                        instante);
            } else movimento.retirar(u);
            u.reservarSaida(null, instante);
            r.encerrar(SituacaoReservaSaida.RETIRADA, instante);
            separacoes.findByReservaId(r.getId()).orElseThrow().encerrar("RETIRADA", instante);
        }
        p.atualizar(SituacaoPedidoSaida.RETIRADO, instante);
        return concluir(p, antes, d.operacaoId(), hash, "RETIRADA_FISICA", d.motivo(), null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao retornarInterno(
            @NotNull @Positive Long id, @NotNull @Valid ExpedicaoDto.Retornar d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        String hash = hash("RETORNO_INTERNO", d);
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        conferir(
                p,
                d.versao(),
                d.resolverPendentes(),
                SituacaoPedidoSaida.RESERVADO,
                SituacaoPedidoSaida.EM_SEPARACAO,
                SituacaoPedidoSaida.SEPARADO);
        if (documentos.findByPedidoIdOrderById(id).stream()
                .anyMatch(doc -> doc.getSituacao().equals("AUTORIZADO")))
            throw conflito(
                    "DOCUMENTO_ATIVO",
                    "Registre cancelamento dos documentos antes de cancelar o pedido.");
        var linhas = ativas(p);
        var dest = destinos(d.unidades());
        var separadas =
                separacoes.buscarDoPedido(id).stream()
                        .filter(s -> s.getSituacao().equals("SEPARADA"))
                        .map(s -> s.getReserva().getId())
                        .collect(Collectors.toSet());
        if (!dest.keySet().equals(separadas))
            throw CadastroSupport.invalido(
                    "Retorno deve confirmar todas e somente as unidades separadas.");
        var antes = detalhe(p);
        for (var r : linhas) {
            var u = r.getUnidade();
            if (u.getReservaSaida() == null || !u.getReservaSaida().getId().equals(id))
                throw conflito("RESERVA_INCONSISTENTE", "Ponte de reserva divergente.");
            if (separadas.contains(r.getId())) {
                var destino = dest.get(r.getId());
                var area =
                        u.isAvariaPosterior() || !u.isCondicaoApta() || u.isBloqueada()
                                ? TipoEndereco.QUARENTENA
                                : TipoEndereco.ARMAZENAGEM;
                movimento.mover(
                        u,
                        destino.destinos(),
                        destino.conjuntoId(),
                        area,
                        d.resolverPendentes(),
                        agora());
                registrarFato(
                        u,
                        d.operacaoId(),
                        "RETORNO_INTERNO",
                        u.getQuantidade(),
                        u.getQuantidade(),
                        agora());
                separacoes.findByReservaId(r.getId()).orElseThrow().encerrar("RETORNADA", agora());
            }
            u.reservarSaida(null, agora());
            r.encerrar(SituacaoReservaSaida.CANCELADA, agora());
        }
        p.atualizar(SituacaoPedidoSaida.CANCELADO, agora());
        return concluir(p, antes, d.operacaoId(), hash, "RETORNO_INTERNO", d.motivo(), null);
    }

    @Transactional
    public ExpedicaoDto.Confirmacao devolver(
            @NotNull @Positive Long id, @NotNull @Valid ExpedicaoDto.Devolver d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(id);
        String hash = hash("DEVOLUCAO_SAIDA", d);
        var replay = repetida(id, d.operacaoId(), hash);
        if (replay != null) return replay;
        conferir(p, d.versao(), d.resolverPendentes(), SituacaoPedidoSaida.RETIRADO);
        var antes = detalhe(p);
        Long novo = retornos.registrar(p, d, hash, agora());
        p.atualizar(p.getSituacao(), agora());
        return concluir(p, antes, d.operacaoId(), hash, "DEVOLUCAO_SAIDA", d.motivo(), novo);
    }

    private void conferir(
            PedidoSaida p, long versao, boolean resolver, SituacaoPedidoSaida... situacoes) {
        versao(p, versao);
        if (!List.of(situacoes).contains(p.getSituacao()))
            throw conflito("SITUACAO_INVALIDA", "Pedido não permite esta operação.");
        resolucao.conferir(resolver, p.getCliente(), p.getArmazem());
        var ids =
                reservas.buscarDoPedido(p.getId()).stream()
                        .map(r -> r.getUnidade().getProduto().getId())
                        .distinct()
                        .sorted()
                        .toList();
        for (Long id : ids)
            resolucao.conferir(
                    resolver,
                    produtos.buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado));
    }

    private void validarIntegral(PedidoSaida p, List<ReservaSaida> linhas) {
        var detalhe = pedidos.detalhe(p);
        if (linhas.isEmpty()
                || detalhe.itens().stream()
                        .anyMatch(
                                i ->
                                        linhas.stream()
                                                        .filter(
                                                                r ->
                                                                        r.getItem()
                                                                                .getId()
                                                                                .equals(i.id()))
                                                        .map(ReservaSaida::getQuantidade)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                                        .compareTo(i.quantidade())
                                                != 0))
            throw conflito("PEDIDO_NAO_INTEGRAL", "Reserva não atende integralmente o pedido.");
    }

    private void validarFisico(PedidoSaida p, ReservaSaida r, TipoEndereco area, boolean resolver) {
        var u = r.getUnidade();
        if (!estoque.elegiveisSaida(List.of(u.getId()), p.getId(), area, resolver)
                        .contains(u.getId())
                || r.getQuantidade().compareTo(u.getQuantidade()) > 0)
            throw conflito(
                    "RESERVA_IMPEDIDA",
                    "Disponibilidade física da reserva impedida; preserve a reserva.");
    }

    private ReservaSaida reserva(PedidoSaida p, Long id) {
        return ativas(p).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private List<ReservaSaida> ativas(PedidoSaida p) {
        return reservas.buscarDoPedido(p.getId()).stream()
                .filter(r -> r.getSituacao() == SituacaoReservaSaida.ATIVA)
                .toList();
    }

    private BigDecimal coberturaAtiva(PedidoSaida p, Long reservaId) {
        return coberturas.buscarDoPedido(p.getId()).stream()
                .filter(
                        c ->
                                c.getReserva().getId().equals(reservaId)
                                        && c.getDocumento().getSituacao().equals("AUTORIZADO")
                                        && c.getDocumento()
                                                .getNatureza()
                                                .equals("RETORNO_MERCADORIA"))
                .map(CoberturaDocumentoSaida::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void conferirComprovantes(PedidoSaida p, List<String> xmls) {
        var docs =
                documentos.findByPedidoIdOrderById(p.getId()).stream()
                        .filter(
                                d ->
                                        d.getSituacao().equals("AUTORIZADO")
                                                && d.getNatureza().equals("RETORNO_MERCADORIA"))
                        .toList();
        var vistos = new HashSet<Long>();
        for (String conteudo : xmls) {
            var n = xml.ler(conteudo);
            var doc =
                    docs.stream()
                            .filter(
                                    d ->
                                            d.getEmitenteCnpj().equals(n.emitente())
                                                    && d.getSerie()
                                                            .equals(Integer.toString(n.serie()))
                                                    && d.getNumero()
                                                            .equals(Long.toString(n.numero()))
                                                    && d.getEmissao().equals(n.emissao())
                                                    && (d.getChaveAcesso() == null
                                                            || d.getChaveAcesso()
                                                                    .equals(n.chaveAcesso())))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            conflito(
                                                    "XML_DIVERGENTE",
                                                    "XML não corresponde a documento ativo deste pedido."));
            if (!vistos.add(doc.getId())) throw CadastroSupport.invalido("Comprovante repetido.");
            var porSku = new HashMap<String, BigDecimal>();
            for (var c : coberturas.buscarDoPedido(p.getId()))
                if (c.getDocumento().getId().equals(doc.getId()))
                    porSku.merge(
                            c.getReserva().getUnidade().getProduto().getSku(),
                            c.getQuantidade(),
                            BigDecimal::add);
            conferirXmlItens(n, porSku, p);
        }
        if (vistos.size() != docs.size())
            throw conflito(
                    "DOCUMENTACAO_INCOMPLETA", "XML de todos os documentos ativos é obrigatório.");
    }

    private void conferirXmlItens(
            NfeEntradaDto n, Map<String, BigDecimal> esperado, PedidoSaida p) {
        var reais = new HashMap<String, BigDecimal>();
        for (var i : n.itens()) {
            String sku = CadastroSupport.codigo(i.sku());
            var r =
                    reservas.buscarDoPedido(p.getId()).stream()
                            .filter(a -> a.getUnidade().getProduto().getSku().equals(sku))
                            .findFirst()
                            .orElseThrow(() -> CadastroSupport.invalido("SKU XML fora do pedido."));
            if (!r.getUnidade()
                    .getProduto()
                    .getUnidadeMedida()
                    .equals(CadastroSupport.codigo(i.unidade())))
                throw CadastroSupport.invalido("Unidade XML divergente.");
            CadastroSupport.quantidade(r.getUnidade().getProduto(), i.quantidade());
            reais.merge(sku, i.quantidade(), BigDecimal::add);
        }
        if (!reais.keySet().equals(esperado.keySet())
                || reais.entrySet().stream()
                        .anyMatch(e -> e.getValue().compareTo(esperado.get(e.getKey())) != 0))
            throw conflito("XML_DIVERGENTE", "Quantidades XML divergem da cobertura declarada.");
    }

    private ExpedicaoDto.Nota nota(NfeEntradaDto n) {
        return new ExpedicaoDto.Nota(
                n.emitente(),
                Integer.toString(n.serie()),
                Long.toString(n.numero()),
                n.emissao(),
                n.chaveAcesso());
    }

    private ExpedicaoDto.Nota normalizar(ExpedicaoDto.Nota n) {
        return new ExpedicaoDto.Nota(
                n.emitenteCnpj(),
                Integer.toString(Integer.parseInt(n.serie())),
                Long.toString(Long.parseLong(n.numero())),
                n.emissao(),
                n.chaveAcesso());
    }

    private ExpedicaoDto.Nota identidade(DocumentoSaida d) {
        return new ExpedicaoDto.Nota(
                d.getEmitenteCnpj(),
                d.getSerie(),
                d.getNumero(),
                d.getEmissao(),
                d.getChaveAcesso());
    }

    private Map<Long, ExpedicaoDto.Destinacao> destinos(List<ExpedicaoDto.Destinacao> lista) {
        var resultado = new HashMap<Long, ExpedicaoDto.Destinacao>();
        for (var d : lista)
            if (resultado.putIfAbsent(d.reservaId(), d) != null)
                throw CadastroSupport.invalido("Reserva com destino repetido.");
        return resultado;
    }

    private void registrarFato(
            UnidadeLogistica u,
            UUID op,
            String tipo,
            BigDecimal antes,
            BigDecimal depois,
            Instant instante) {
        BigDecimal equiv = BigDecimal.valueOf(u.getPosicoesEquivalentes());
        fatos.save(
                new FatoPermanencia(
                        u,
                        op.toString(),
                        tipo,
                        instante,
                        agora(),
                        antes,
                        depois,
                        equiv,
                        depois.signum() == 0 ? BigDecimal.ZERO : equiv));
    }

    private void autorizar(PedidoSaida p) {
        acesso.cliente(p.getCliente().getId());
        acesso.armazem(p.getArmazem().getId());
    }

    private ExpedicaoDto.Detalhe detalhe(PedidoSaida p) {
        autorizar(p);
        var cob = coberturas.buscarDoPedido(p.getId());
        return new ExpedicaoDto.Detalhe(
                pedidos.detalhe(p),
                separacoes.buscarDoPedido(p.getId()).stream()
                        .map(
                                s ->
                                        new ExpedicaoDto.Separacao(
                                                s.getId(),
                                                s.getReserva().getId(),
                                                s.getSituacao(),
                                                s.getRevisaoConteudoLida(),
                                                s.getLidaEm(),
                                                s.getSeparadaEm(),
                                                s.getEncerradaEm(),
                                                s.getOrigensJson() == null
                                                        ? List.of()
                                                        : Arrays.asList(
                                                                mapper.readValue(
                                                                        s.getOrigensJson(),
                                                                        EstoqueDto.Destino[]
                                                                                .class)),
                                                s.getConjuntoOrigem() == null
                                                        ? null
                                                        : s.getConjuntoOrigem().getId()))
                        .toList(),
                documentos.findByPedidoIdOrderById(p.getId()).stream()
                        .map(
                                d ->
                                        new ExpedicaoDto.DocumentoRegistrado(
                                                d.getId(),
                                                d.getOrigem(),
                                                d.getNatureza(),
                                                identidade(d),
                                                d.getProtocolo(),
                                                d.getSituacao(),
                                                d.getRegistradoEm(),
                                                d.getCanceladoEm(),
                                                cob.stream()
                                                        .filter(
                                                                c ->
                                                                        c.getDocumento()
                                                                                .getId()
                                                                                .equals(d.getId()))
                                                        .map(
                                                                c ->
                                                                        new ExpedicaoDto.Cobertura(
                                                                                c.getReserva()
                                                                                        .getId(),
                                                                                c.getReserva()
                                                                                        .getUnidade()
                                                                                        .getNota()
                                                                                        .getId(),
                                                                                c.getReserva()
                                                                                        .getUnidade()
                                                                                        .getProduto()
                                                                                        .getSku(),
                                                                                c.getQuantidade()
                                                                                        .setScale(
                                                                                                6)))
                                                        .toList()))
                        .toList(),
                retiradas.findByPedidoId(p.getId()).map(RetiradaSaida::getRetiradaEm).orElse(null),
                baixas.buscarDoPedido(p.getId()).stream()
                        .map(
                                b ->
                                        new ExpedicaoDto.Baixa(
                                                b.getId(),
                                                b.getReserva().getId(),
                                                b.getEntradaOrigem().getId(),
                                                b.getQuantidade().setScale(6),
                                                b.getDataFifo(),
                                                b.getInicioArmazenagemEm(),
                                                b.getPosicoesEquivalentes().setScale(6)))
                        .toList(),
                devolucoes.buscarDoPedido(p.getId()).stream()
                        .map(
                                d ->
                                        new ExpedicaoDto.Devolucao(
                                                d.getId(),
                                                d.getBaixa().getId(),
                                                d.getPedidoEntrada().getId(),
                                                d.getEntradaNova().getId(),
                                                d.getQuantidade().setScale(6),
                                                d.getEntradaNova().getDataFifo(),
                                                d.getRegistradaEm()))
                        .toList(),
                fatos.buscarDoPedido(p.getId()).stream()
                        .map(
                                f ->
                                        new ExpedicaoDto.Fato(
                                                f.getId(),
                                                f.getUnidade().getId(),
                                                f.getTipo(),
                                                f.getOcorridaEm(),
                                                f.getRegistradaEm(),
                                                f.getQuantidadeAntes().setScale(6),
                                                f.getQuantidadeDepois().setScale(6),
                                                f.getEquivalenciaAntes().setScale(6),
                                                f.getEquivalenciaDepois().setScale(6)))
                        .toList());
    }

    private ExpedicaoDto.Confirmacao concluir(
            PedidoSaida p,
            ExpedicaoDto.Detalhe antes,
            UUID op,
            String hash,
            String acao,
            String motivo,
            Long novo) {
        unidades.flush();
        pedidoRepository.flush();
        var resultado = new ExpedicaoDto.Confirmacao(op, detalhe(p), novo);
        auditoria.registrar(
                "PEDIDO_SAIDA", p.getId(), acao, CadastroSupport.motivo(motivo), antes, resultado);
        operacoes.saveAndFlush(
                new OperacaoSaida(
                        p,
                        op.toString(),
                        hash,
                        acao,
                        acesso.usuario(),
                        CadastroSupport.motivo(motivo),
                        agora(),
                        mapper.writeValueAsString(resultado)));
        return resultado;
    }

    private ExpedicaoDto.Confirmacao repetida(Long id, UUID op, String hash) {
        var a = operacoes.findByPedidoIdAndOperacaoId(id, op.toString());
        if (a.isEmpty()) return null;
        if (!a.get().getConteudoHash().equals(hash))
            throw conflito(
                    "OPERACAO_REUTILIZADA", "Identificador já usado com conteúdo divergente.");
        return mapper.readValue(a.get().getResultado(), ExpedicaoDto.Confirmacao.class);
    }

    private void versao(PedidoSaida p, long v) {
        if (p.getVersao() != v)
            throw conflito("VERSAO_DESATUALIZADA", "Pedido alterado; consulte novamente.");
    }

    private String hash(String tipo, Object d) {
        return NfeXmlService.hash(tipo + ":" + mapper.writeValueAsString(d));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static RegraNegocioException conflito(String c, String m) {
        return RegraNegocioException.conflito(c, m);
    }
}
