package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.AvariaDto;
import br.com.rodogarcia.wms.dto.ContagemDto;
import br.com.rodogarcia.wms.dto.ContingenciaDto;
import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.DependenciaContingencia;
import br.com.rodogarcia.wms.models.LinhaContingencia;
import br.com.rodogarcia.wms.models.OperacaoSaida;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.SituacaoContingencia;
import br.com.rodogarcia.wms.models.TipoContingencia;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ChegadaRecebimentoRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ContagemEstoqueRepository;
import br.com.rodogarcia.wms.repositories.DependenciaContingenciaRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.LinhaContingenciaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.OperacaoAdministrativaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.PedidoEntradaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.RetiradaSaidaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ContingenciaService {
    private final LinhaContingenciaRepository linhas;
    private final DependenciaContingenciaRepository dependencias;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final PedidoEntradaRepository entradas;
    private final PedidoSaidaRepository saidas;
    private final UnidadeLogisticaRepository unidades;
    private final ChegadaRecebimentoRepository chegadas;
    private final OperacaoSaidaRepository operacoesSaida;
    private final MovimentoEstoqueRepository movimentos;
    private final OperacaoAdministrativaRepository registros;
    private final RetiradaSaidaRepository retiradas;
    private final RecebimentoService recebimento;
    private final MovimentacaoEstoqueService movimentacao;
    private final ExpedicaoService expedicao;
    private final PedidoSaidaService pedidosSaida;
    private final AvariaService avarias;
    private final FatoServicoService fatos;
    private final ContagemEstoqueService contagens;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;
    private final TransactionTemplate transacao;
    private final EntityManager em;
    private final EntradaConferidaRepository conferidas;
    private final ContagemEstoqueRepository leituras;

    public ContingenciaService(
            LinhaContingenciaRepository linhas,
            DependenciaContingenciaRepository dependencias,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            PedidoEntradaRepository entradas,
            PedidoSaidaRepository saidas,
            UnidadeLogisticaRepository unidades,
            ChegadaRecebimentoRepository chegadas,
            OperacaoSaidaRepository operacoesSaida,
            MovimentoEstoqueRepository movimentos,
            OperacaoAdministrativaRepository registros,
            RetiradaSaidaRepository retiradas,
            RecebimentoService recebimento,
            MovimentacaoEstoqueService movimentacao,
            ExpedicaoService expedicao,
            PedidoSaidaService pedidosSaida,
            AvariaService avarias,
            FatoServicoService fatos,
            ContagemEstoqueService contagens,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock,
            EntityManager em,
            EntradaConferidaRepository conferidas,
            ContagemEstoqueRepository leituras,
            PlatformTransactionManager transactions) {
        this.conferidas = conferidas;
        this.leituras = leituras;
        this.em = em;
        this.linhas = linhas;
        this.dependencias = dependencias;
        this.clientes = clientes;
        this.armazens = armazens;
        this.entradas = entradas;
        this.saidas = saidas;
        this.unidades = unidades;
        this.chegadas = chegadas;
        this.operacoesSaida = operacoesSaida;
        this.movimentos = movimentos;
        this.registros = registros;
        this.retiradas = retiradas;
        this.recebimento = recebimento;
        this.movimentacao = movimentacao;
        this.expedicao = expedicao;
        this.pedidosSaida = pedidosSaida;
        this.avarias = avarias;
        this.fatos = fatos;
        this.contagens = contagens;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
        transacao = new TransactionTemplate(transactions);
    }

    @Transactional
    public ContingenciaDto.Resultado registrar(@NotNull @Valid ContingenciaDto.Registrar d) {
        acesso.exigirSupervisor();
        autorizar(d.clienteId(), d.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(d.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(d.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (cliente.getSituacao() != SituacaoCadastro.ATIVO
                || armazem.getSituacao() != SituacaoCadastro.ATIVO) acesso.exigirGestor();
        if (cliente.getSituacao() == SituacaoCadastro.INATIVO
                || armazem.getSituacao() == SituacaoCadastro.INATIVO)
            throw conflito(
                    "CADASTRO_INATIVO",
                    "Contingência física não cria compromisso em cadastro inativo.");
        if (d.ocorridaEm().isAfter(agora()))
            throw CadastroSupport.invalido("Instante real não pode ser futuro.");
        String identidade = CadastroSupport.codigo(d.identidadeFato());
        var deps = d.dependencias().stream().map(CadastroSupport::codigo).sorted().toList();
        if (new HashSet<>(deps).size() != deps.size() || deps.contains(identidade))
            throw conflito("DEPENDENCIA_CIRCULAR", "Dependências repetidas ou da própria linha.");
        Map<String, Object> dados = canonico(d.dados());
        var conteudo = new ContingenciaDto.Conteudo(dados, deps, d.efeitoRegistradoNoWms());
        String json = mapper.writeValueAsString(conteudo);
        if (json.length() > 3000000)
            throw CadastroSupport.invalido("Conteúdo de linha excede 3 milhões de caracteres.");
        String hash =
                operacoes.hash(
                        "FATO_CONTINGENCIA",
                        identidade,
                        List.of(
                                d.clienteId(),
                                d.armazemId(),
                                d.tipo(),
                                d.ocorridaEm().truncatedTo(ChronoUnit.MICROS),
                                CadastroSupport.texto(d.operador()),
                                CadastroSupport.motivo(d.fonte()),
                                conteudo));
        var replay = operacoes.repetida(d.operacaoId(), hash, ContingenciaDto.Resultado.class);
        if (replay != null) return replay;
        var existente = linhas.findByIdentidadeFato(identidade).orElse(null);
        if (existente != null) {
            autorizar(existente);
            if (!existente.getConteudoHash().equals(hash))
                throw conflito(
                        "FATO_CONTINGENCIA_DIVERGENTE",
                        "Identidade global já representa outro conteúdo/contexto/tipo.");
            var registroOriginal =
                    registros
                            .findFirstByTipoAndRecursoIdOrderByIdAsc(
                                    "REGISTRO_CONTINGENCIA", existente.getId())
                            .orElseThrow(
                                    () ->
                                            conflito(
                                                    "PROVA_OPERACIONAL_AUSENTE",
                                                    "Linha existente sem confirmação original de registro."));
            var resposta =
                    mapper.readValue(
                            registroOriginal.getResultado(), ContingenciaDto.Resultado.class);
            operacoes.salvar(
                    d.operacaoId(),
                    "REGISTRO_CONTINGENCIA",
                    cliente,
                    armazem,
                    existente.getId(),
                    hash,
                    resposta);
            return resposta;
        }
        var l =
                linhas.saveAndFlush(
                        new LinhaContingencia(
                                identidade,
                                cliente,
                                armazem,
                                d.tipo(),
                                json,
                                hash,
                                d.ocorridaEm().truncatedTo(ChronoUnit.MICROS),
                                agora(),
                                CadastroSupport.texto(d.operador()),
                                CadastroSupport.motivo(d.fonte())));
        conferirGrafo(l, new HashSet<>(), new HashSet<>());
        var resposta = resultado(l);
        auditoria.registrar(
                "CONTINGENCIA",
                l.getId(),
                "REGISTRO_CONTINGENCIA",
                CadastroSupport.motivo(d.motivo()),
                null,
                resposta);
        operacoes.salvar(
                d.operacaoId(),
                "REGISTRO_CONTINGENCIA",
                cliente,
                armazem,
                l.getId(),
                hash,
                resposta);
        return resposta;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ContingenciaDto.Resultado conciliar(
            @NotNull @Positive Long id, @NotNull @Valid ContingenciaDto.Conciliar d) {
        acesso.exigirSupervisor();
        // O efeito e a marca pertencem à primeira transação. Falha desfaz ambos; a segunda
        // conserva somente a pendência identificada, sem consumir a chave de confirmação.
        try {
            return transacao.execute(status -> conciliarAtomico(id, d));
        } catch (RegraNegocioException e) {
            if (e.getStatus() != HttpStatus.CONFLICT
                    || Set.of("OPERACAO_DIVERGENTE", "VERSAO_DESATUALIZADA", "FATO_JA_CONCILIADO")
                            .contains(e.getCodigo())) throw e;
            return transacao.execute(
                    status -> {
                        var l = bloquearLinha(id);
                        guardaHistorica(l);
                        if (l.getSituacao() == SituacaoContingencia.CONCILIADA) throw e;
                        var antes = resultado(l);
                        l.pendente(e.getCodigo());
                        linhas.flush();
                        var resposta = resultado(l);
                        auditoria.registrar(
                                "CONTINGENCIA",
                                id,
                                "CONCILIACAO_CONTINGENCIA",
                                CadastroSupport.motivo(d.motivo()),
                                antes,
                                resposta);
                        return resposta;
                    });
        }
    }

    private ContingenciaDto.Resultado conciliarAtomico(Long id, ContingenciaDto.Conciliar d) {
        var l = bloquearLinha(id);
        guardaHistorica(l);
        String hash = operacoes.hash("CONCILIACAO_CONTINGENCIA", id, d);
        var repetida = operacoes.repetida(d.operacaoId(), hash, ContingenciaDto.Resultado.class);
        if (repetida != null) return repetida;
        if (l.getVersao() != d.versao())
            throw conflito("VERSAO_DESATUALIZADA", "Consulte a linha antes da conciliação.");
        if (l.getSituacao() != SituacaoContingencia.PENDENTE)
            throw conflito(
                    "FATO_JA_CONCILIADO", "Fato já possui efeito comprovado, sem outra execução.");
        conferirGrafo(l, new HashSet<>(), new HashSet<>());
        for (String identidade : conteudo(l).dependencias()) {
            var depende =
                    linhas.findByIdentidadeFato(identidade)
                            .orElseThrow(
                                    () ->
                                            conflito(
                                                    "DEPENDENCIA_AUSENTE",
                                                    "Dependência ainda não registrada."));
            contexto(l, depende.getCliente().getId(), depende.getArmazem().getId());
            if (depende.getOcorridaEm().isAfter(l.getOcorridaEm()))
                throw conflito(
                        "ORDEM_CONTINGENCIA_DIVERGENTE",
                        "Dependência ocorre após o fato dependente.");
            if (depende.getSituacao() != SituacaoContingencia.CONCILIADA)
                throw conflito(
                        "DEPENDENCIA_PENDENTE", "Concilie primeiro a dependência comprovada.");
            if (!dependencias.existsByLinhaIdAndDependeId(id, depende.getId()))
                dependencias.saveAndFlush(new DependenciaContingencia(l, depende));
        }
        var c = conteudo(l);
        if (c.efeitoRegistradoNoWms() && !"VINCULAR".equals(d.modo()))
            throw conflito(
                    "EFEITO_EXISTENTE_EXIGE_VINCULO",
                    "Fato executado operacionalmente só pode vincular resultado comprovado.");
        if ("VINCULAR".equals(d.modo()) && d.prova() == null)
            throw conflito(
                    "PROVA_OPERACIONAL_AUSENTE", "Informe operação original e hash comprovados.");
        var antes = resultado(l);
        UUID operacaoEfeito =
                "VINCULAR".equals(d.modo())
                        ? d.prova().operacaoOriginal()
                        : UUID.nameUUIDFromBytes(
                                ("CONTINGENCIA:" + l.getIdentidadeFato())
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        for (var outra :
                linhas.findByClienteIdAndArmazemIdAndSituacao(
                        l.getCliente().getId(),
                        l.getArmazem().getId(),
                        SituacaoContingencia.CONCILIADA)) {
            var snapshot = mapper.readTree(outra.getResultadoJson());
            var anterior = snapshot.get("operacaoEfeito");
            var provaAnterior = snapshot.get("prova");
            boolean outraOperacao =
                    d.prova() != null
                            && provaAnterior != null
                            && !provaAnterior.isNull()
                            && !d.prova()
                                    .conteudoHash()
                                    .equals(provaAnterior.get("conteudoHash").asString());
            if (!outraOperacao
                    && anterior != null
                    && operacaoEfeito.toString().equals(anterior.asString()))
                throw conflito(
                        "EFEITO_CONTINGENCIA_JA_VINCULADO",
                        "Operação comprovada já pertence a outra identidade de fato.");
        }
        Object efeito = aplicar(l, d);
        var memoria = new TreeMap<String, Object>();
        memoria.put("modo", d.modo());
        memoria.put("identidadeFato", l.getIdentidadeFato());
        memoria.put("ocorridaEm", l.getOcorridaEm());
        memoria.put("operacaoEfeito", operacaoEfeito);
        memoria.put("prova", d.prova());
        memoria.put("efeito", efeito);
        l.conciliar(mapper.writeValueAsString(memoria), agora());
        linhas.flush();
        var resposta = resultado(l);
        auditoria.registrar(
                "CONTINGENCIA",
                id,
                "CONCILIACAO_CONTINGENCIA",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resposta);
        operacoes.salvar(
                d.operacaoId(),
                "CONCILIACAO_CONTINGENCIA",
                l.getCliente(),
                l.getArmazem(),
                id,
                hash,
                resposta);
        return resposta;
    }

    private Object aplicar(LinhaContingencia l, ContingenciaDto.Conciliar comando) {
        var c = conteudo(l);
        var raiz = c.dados();
        var alvo = alvoOperacional(l);
        UUID op =
                "VINCULAR".equals(comando.modo())
                        ? comando.prova().operacaoOriginal()
                        : UUID.nameUUIDFromBytes(
                                ("CONTINGENCIA:" + l.getIdentidadeFato())
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var dados = new TreeMap<String, Object>(mapa(raiz.get("dados")));
        dados.put("operacaoId", op);
        boolean vincular = "VINCULAR".equals(comando.modo());
        Long pedido = alvo.pedido();
        UUID codigo = alvo.codigoUnidade();
        String json = mapper.writeValueAsString(dados);
        switch (l.getTipo()) {
            case CHEGADA -> {
                if (pedido == null) throw CadastroSupport.invalido("Informe pedidoId de entrada.");
                var dto = mapper.readValue(json, RecebimentoDto.RegistrarChegada.class);
                instante(l, dto.chegouEm());
                if (vincular) {
                    var original =
                            chegadas.findByPedidoIdAndOperacaoId(pedido, op.toString())
                                    .orElseThrow(
                                            () ->
                                                    conflito(
                                                            "PROVA_OPERACIONAL_AUSENTE",
                                                            "Chegada original não comprovada."));
                    prova(comando, original.getConteudoHash());
                    instante(l, original.getChegouEm());
                }
                return recebimento.registrarChegada(pedido, dto);
            }
            case REMANEJAMENTO -> {
                if (codigo == null) throw CadastroSupport.invalido("Informe codigoUnidade.");
                var dto = mapper.readValue(json, EstoqueDto.Posicionar.class);
                if (vincular) {
                    var original =
                            movimentos
                                    .findByPedidoIdAndOperacaoId(pedido, op.toString())
                                    .orElseThrow(
                                            () ->
                                                    conflito(
                                                            "PROVA_OPERACIONAL_AUSENTE",
                                                            "Movimento original ausente."));
                    prova(comando, original.getConteudoHash());
                    if (!Set.of("MOVIMENTACAO", "ENDERECAMENTO").contains(original.getAcao()))
                        throw conflito(
                                "PROVA_OPERACIONAL_DIVERGENTE", "Operação não é remanejamento.");
                    instante(l, original.getInstante());
                    if (raiz.get("identidadeExecucao") != null)
                        return movimentacao.posicionarContingencia(
                                codigo,
                                dto,
                                l.getOcorridaEm(),
                                CadastroSupport.codigo(raiz.get("identidadeExecucao").toString()));
                    return movimentacao.posicionar(codigo, dto);
                }
                return movimentacao.posicionarContingencia(
                        codigo, dto, l.getOcorridaEm(), l.getIdentidadeFato());
            }
            case AVARIA -> {
                if (codigo == null) throw CadastroSupport.invalido("Informe codigoUnidade.");
                var dto = mapper.readValue(json, AvariaDto.Registrar.class);
                instante(l, dto.ocorridaEm());
                if (vincular) {
                    var original =
                            movimentos
                                    .findByPedidoIdAndOperacaoId(pedido, op.toString())
                                    .orElseThrow(
                                            () ->
                                                    conflito(
                                                            "PROVA_OPERACIONAL_AUSENTE",
                                                            "Dano original não comprovado."));
                    prova(comando, original.getConteudoHash());
                    if (!"AVARIA_DETALHADA".equals(original.getAcao()))
                        throw conflito(
                                "PROVA_OPERACIONAL_DIVERGENTE", "Operação não é avaria detalhada.");
                }
                return avarias.registrar(codigo, dto);
            }
            case FATO_SERVICO -> {
                var dto = mapper.readValue(json, FatoServicoDto.Registrar.class);
                contexto(l, dto.clienteId(), dto.armazemId());
                instante(l, dto.executadoEm());
                if (vincular) registro(comando, op, "REGISTRO_SERVICO");
                return fatos.registrar(dto);
            }
            case CONTAGEM -> {
                var dto = mapper.readValue(json, ContagemDto.Contar.class);
                instante(l, dto.observadoEm());
                var u =
                        unidades.findByCodigo(dto.codigoUnidade().toString())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                contexto(l, u.getPedido().getCliente().getId(), u.getPedido().getArmazem().getId());
                if (vincular) registro(comando, op, "LEITURA_CONTAGEM");
                return contagens.contar(dto);
            }
            case RETIRADA -> {
                if (pedido == null) throw CadastroSupport.invalido("Informe pedidoId de saída.");
                var dto = mapper.readValue(json, ExpedicaoDto.Retirar.class);
                if (vincular) {
                    operacaoSaida(comando, pedido, op, "RETIRADA_FISICA");
                    instante(
                            l,
                            retiradas
                                    .findByPedidoId(pedido)
                                    .orElseThrow(
                                            () ->
                                                    conflito(
                                                            "PROVA_OPERACIONAL_AUSENTE",
                                                            "Retirada original ausente."))
                                    .getRetiradaEm());
                    if (raiz.get("identidadeExecucao") != null)
                        return expedicao.retirarContingencia(
                                pedido,
                                dto,
                                l.getOcorridaEm(),
                                CadastroSupport.codigo(raiz.get("identidadeExecucao").toString()));
                    return expedicao.retirar(pedido, dto);
                }
                return expedicao.retirarContingencia(
                        pedido, dto, l.getOcorridaEm(), l.getIdentidadeFato());
            }
            case RESERVA, SEPARACAO, RETORNO -> {
                if (!vincular || pedido == null)
                    throw conflito(
                            "HISTORICO_CONTINGENCIA_INSUFICIENTE",
                            "Este efeito exige operação existente comprovada, não data de conciliação como fato passado.");
                String tipo =
                        l.getTipo() == TipoContingencia.RESERVA
                                ? "RESERVA_SAIDA"
                                : l.getTipo() == TipoContingencia.SEPARACAO
                                        ? "SEPARACAO_SAIDA"
                                        : "RETORNO_INTERNO";
                var original = operacaoSaida(comando, pedido, op, tipo);
                // O resultado operacional imutável comprova o instante de separação/retorno;
                // reserva também conserva criadaEm na linha de estoque.
                if (l.getTipo() == TipoContingencia.RESERVA) {
                    var rs =
                            mapper.readValue(
                                    original.getResultado(), PedidoSaidaDto.Confirmacao.class);
                    if (rs.pedido().reservas().isEmpty())
                        throw conflito("PROVA_OPERACIONAL_AUSENTE", "Reserva original sem linhas.");
                    instante(l, rs.pedido().reservas().getFirst().criadaEm());
                    return pedidosSaida.reservar(
                            pedido, mapper.readValue(json, PedidoSaidaDto.Reservar.class));
                }
                var snapshot =
                        mapper.readValue(original.getResultado(), ExpedicaoDto.Confirmacao.class);
                if (snapshot.expedicao().separacoes().isEmpty())
                    throw conflito(
                            "HISTORICO_CONTINGENCIA_INSUFICIENTE",
                            "Snapshot operacional sem marcos de separação/retorno.");
                for (var s : snapshot.expedicao().separacoes())
                    instante(
                            l,
                            l.getTipo() == TipoContingencia.SEPARACAO
                                    ? s.separadaEm()
                                    : s.encerradaEm());
                if (l.getTipo() == TipoContingencia.SEPARACAO)
                    return expedicao.separar(
                            pedido, mapper.readValue(json, ExpedicaoDto.Separar.class));
                return expedicao.retornarInterno(
                        pedido, mapper.readValue(json, ExpedicaoDto.Retornar.class));
            }
            case ENTRADA -> {
                if (pedido == null) throw CadastroSupport.invalido("Informe pedidoId de entrada.");
                // Efetivar conserva o contrato anterior, sem UUID no DTO; a execução temporal
                // recebe a chave global separadamente, sem campo técnico estranho ao payload.
                dados.remove("operacaoId");
                var dto =
                        mapper.readValue(
                                mapper.writeValueAsString(dados), PedidoEntradaDto.Efetivar.class);
                var p = entradas.findById(pedido).orElseThrow(RegraNegocioException::naoEncontrado);
                contexto(l, p.getCliente().getId(), p.getArmazem().getId());
                if (vincular) {
                    registro(comando, op, "ENTRADA_CONTINGENCIA");
                    var es =
                            conferidas
                                    .findByItemChegadaChegadaPedidoId(
                                            pedido,
                                            org.springframework.data.domain.Pageable.unpaged())
                                    .getContent();
                    if (es.isEmpty())
                        throw conflito(
                                "PROVA_OPERACIONAL_AUSENTE",
                                "Entrada original sem conferência efetivada.");
                    for (var e : es) instante(l, e.getEfetivadaEm());
                    Object identidade = raiz.get("identidadeExecucao");
                    if (identidade == null)
                        throw conflito(
                                "PROVA_OPERACIONAL_AUSENTE",
                                "Informe identidade da execução temporal original.");
                    return recebimento.efetivarContingencia(
                            pedido,
                            dto,
                            op,
                            l.getOcorridaEm(),
                            CadastroSupport.codigo(identidade.toString()));
                }
                return recebimento.efetivarContingencia(
                        pedido, dto, op, l.getOcorridaEm(), l.getIdentidadeFato());
            }
            case AJUSTE -> {
                Long contagem = alvo.contagem();
                var cOriginal =
                        leituras.findById(contagem)
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                contexto(
                        l,
                        cOriginal.getUnidade().getPedido().getCliente().getId(),
                        cOriginal.getUnidade().getPedido().getArmazem().getId());
                var dto = mapper.readValue(json, ContagemDto.Aplicar.class);
                if (vincular) {
                    registro(comando, op, "APLICACAO_CONTAGEM");
                    var r =
                            mapper.readValue(
                                    registros
                                            .findByOperacaoId(op.toString())
                                            .orElseThrow()
                                            .getResultado(),
                                    ContagemDto.Resultado.class);
                    if (r.efeitoJson() == null)
                        throw conflito(
                                "PROVA_OPERACIONAL_AUSENTE",
                                "Ajuste original sem efeito identificado.");
                    var marco = mapper.readTree(r.efeitoJson()).get("efetivadoEm");
                    instante(l, marco == null ? null : Instant.parse(marco.asString()));
                    dto = conferirEscalaOriginal(dto, r.efeitoJson());
                    if (raiz.get("identidadeExecucao") != null)
                        return contagens.aplicarContingencia(
                                contagem,
                                dto,
                                l.getOcorridaEm(),
                                CadastroSupport.codigo(raiz.get("identidadeExecucao").toString()));
                    return contagens.aplicar(contagem, dto);
                }
                return contagens.aplicarContingencia(
                        contagem, dto, l.getOcorridaEm(), l.getIdentidadeFato());
            }
        }
        throw CadastroSupport.invalido("Tipo de contingência inválido.");
    }

    private ContagemDto.Aplicar conferirEscalaOriginal(ContagemDto.Aplicar dto, String efeitoJson) {
        Map<String, Object> efeito =
                mapper.readerFor(new TypeReference<Map<String, Object>>() {})
                        .with(
                                tools.jackson.databind.DeserializationFeature
                                        .USE_BIG_DECIMAL_FOR_FLOATS)
                        .readValue(efeitoJson);
        var historicas = (List<?>) efeito.get("origens");
        var deltas = new ArrayList<ContagemDto.DeltaOrigem>();
        for (var d : dto.origens()) {
            var original =
                    historicas.stream()
                            .map(v -> (Map<?, ?>) v)
                            .filter(v -> ((Number) v.get("entradaId")).longValue() == d.entradaId())
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            conflito(
                                                    "PROVA_OPERACIONAL_DIVERGENTE",
                                                    "Origem não consta do ajuste comprovado."));
            var quantidade = new BigDecimal(original.get("quantidade").toString());
            try {
                deltas.add(
                        new ContagemDto.DeltaOrigem(
                                d.entradaId(),
                                d.delta()
                                        .setScale(
                                                quantidade.scale(),
                                                java.math.RoundingMode.UNNECESSARY)));
            } catch (ArithmeticException e) {
                throw conflito(
                        "PROVA_OPERACIONAL_DIVERGENTE",
                        "Delta não corresponde à precisão original comprovada.");
            }
        }
        return new ContagemDto.Aplicar(
                dto.operacaoId(),
                dto.revisao(),
                dto.versaoUnidade(),
                dto.motivo(),
                dto.causa(),
                dto.destino(),
                dto.comprovacao(),
                deltas);
    }

    private void registro(ContingenciaDto.Conciliar d, UUID op, String tipo) {
        var r =
                registros
                        .findByOperacaoId(op.toString())
                        .orElseThrow(
                                () ->
                                        conflito(
                                                "PROVA_OPERACIONAL_AUSENTE",
                                                "Registro original ausente."));
        prova(d, r.getConteudoHash());
        if (!r.getTipo().equals(tipo))
            throw conflito("PROVA_OPERACIONAL_DIVERGENTE", "Ação original difere do fato.");
    }

    private OperacaoSaida operacaoSaida(
            ContingenciaDto.Conciliar d, Long pedido, UUID op, String tipo) {
        var r =
                operacoesSaida
                        .findByPedidoIdAndOperacaoId(pedido, op.toString())
                        .orElseThrow(
                                () ->
                                        conflito(
                                                "PROVA_OPERACIONAL_AUSENTE",
                                                "Operação original ausente."));
        prova(d, r.getConteudoHash());
        if (!r.getTipo().equals(tipo))
            throw conflito("PROVA_OPERACIONAL_DIVERGENTE", "Ação original difere do fato.");
        return r;
    }

    private static void prova(ContingenciaDto.Conciliar d, String hash) {
        if (!hash.equals(d.prova().conteudoHash()))
            throw conflito(
                    "PROVA_OPERACIONAL_DIVERGENTE", "Hash original não corresponde à comprovação.");
    }

    private void instante(LinhaContingencia l, Instant instante) {
        if (instante == null || !l.getOcorridaEm().equals(instante.truncatedTo(ChronoUnit.MICROS)))
            throw conflito(
                    "INSTANTE_CONTINGENCIA_DIVERGENTE",
                    "Instante do comando/prova difere do fato original.");
    }

    private void conferirGrafo(LinhaContingencia l, Set<String> pilha, Set<String> completos) {
        if (completos.contains(l.getIdentidadeFato())) return;
        if (!pilha.add(l.getIdentidadeFato()))
            throw conflito(
                    "DEPENDENCIA_CIRCULAR", "Ciclo entre identidades originais da contingência.");
        for (String id : conteudo(l).dependencias()) {
            var outra = linhas.findByIdentidadeFato(id).orElse(null);
            if (outra != null) {
                contexto(l, outra.getCliente().getId(), outra.getArmazem().getId());
                conferirGrafo(outra, pilha, completos);
            }
        }
        pilha.remove(l.getIdentidadeFato());
        completos.add(l.getIdentidadeFato());
    }

    private void guardaHistorica(LinhaContingencia l) {
        var d = mapa(conteudo(l).dados().get("dados"));
        if (Boolean.TRUE.equals(d.get("resolverPendentes"))
                || d.get("resolucao") != null
                || l.getCliente().getSituacao() != SituacaoCadastro.ATIVO
                || l.getArmazem().getSituacao() != SituacaoCadastro.ATIVO) acesso.exigirGestor();
    }

    private LinhaContingencia bloquearLinha(Long id) {
        var l = linhas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(l);
        var alvo = alvoOperacional(l);
        Long entrada = alvo.entradaParaLock();
        if (entrada != null) {
            var p = entradas.findById(entrada).orElseThrow(RegraNegocioException::naoEncontrado);
            contexto(l, p.getCliente().getId(), p.getArmazem().getId());
            entradas.buscarParaAtualizar(entrada).orElseThrow(RegraNegocioException::naoEncontrado);
        }
        var cliente =
                clientes.buscarParaAtualizar(l.getCliente().getId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(l.getArmazem().getId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        em.refresh(cliente);
        em.refresh(armazem);
        em.refresh(l, LockModeType.PESSIMISTIC_WRITE);
        guardaHistorica(l);
        if (cliente.getSituacao() == SituacaoCadastro.INATIVO
                || armazem.getSituacao() == SituacaoCadastro.INATIVO)
            throw conflito(
                    "CADASTRO_INATIVO",
                    "Contingência física não cria compromisso em cadastro inativo.");
        return l;
    }

    private record AlvoOperacional(
            Long pedido, UUID codigoUnidade, Long contagem, Long entradaParaLock) {}

    private AlvoOperacional alvoOperacional(LinhaContingencia l) {
        var raiz = conteudo(l).dados();
        var dados = mapa(raiz.get("dados"));
        Long pedido = raiz.get("pedidoId") == null ? null : numero(raiz.get("pedidoId"));
        UUID codigo = codigoUnidade(raiz.get("codigoUnidade"));
        Long contagem = raiz.get("contagemId") == null ? null : numero(raiz.get("contagemId"));
        Long entradaParaLock = null;
        switch (l.getTipo()) {
            case CONTAGEM -> {
                var dto = mapper.convertValue(dados, ContagemDto.Contar.class);
                if (dto.codigoUnidade() == null)
                    throw CadastroSupport.invalido("Informe codigoUnidade do comando de contagem.");
                conferirReferencia(codigo, dto.codigoUnidade());
                codigo = dto.codigoUnidade();
                Long entrada = pedidoDaUnidade(l, codigo);
                conferirReferencia(pedido, entrada);
                pedido = entrada;
                entradaParaLock = entrada;
            }
            case REMANEJAMENTO, AVARIA -> {
                if (codigo == null) throw CadastroSupport.invalido("Informe codigoUnidade.");
                Long entrada = pedidoDaUnidade(l, codigo);
                conferirReferencia(pedido, entrada);
                pedido = entrada;
                if (l.getTipo() == TipoContingencia.REMANEJAMENTO) entradaParaLock = entrada;
            }
            case AJUSTE -> {
                if (contagem == null) throw CadastroSupport.invalido("Informe contagemId.");
                var c =
                        leituras.findById(contagem)
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                UUID contado = UUID.fromString(c.getUnidade().getCodigo());
                conferirReferencia(codigo, contado);
                codigo = contado;
                Long entrada = pedidoDaUnidade(l, codigo);
                conferirReferencia(pedido, entrada);
                pedido = entrada;
                entradaParaLock = entrada;
            }
            case CHEGADA, ENTRADA -> {
                if (pedido == null) throw CadastroSupport.invalido("Informe pedidoId de entrada.");
                conferirPedidoEntrada(l, pedido);
                if (codigo != null) conferirReferencia(pedido, pedidoDaUnidade(l, codigo));
                entradaParaLock = pedido;
            }
            case RESERVA, SEPARACAO, RETIRADA, RETORNO -> {
                if (pedido == null) throw CadastroSupport.invalido("Informe pedidoId de saída.");
                var p = saidas.findById(pedido).orElseThrow(RegraNegocioException::naoEncontrado);
                contexto(l, p.getCliente().getId(), p.getArmazem().getId());
                if (codigo != null) pedidoDaUnidade(l, codigo);
            }
            case FATO_SERVICO -> {
                var dto = mapper.convertValue(dados, FatoServicoDto.Registrar.class);
                contexto(l, dto.clienteId(), dto.armazemId());
                Long entrada = dto.pedidoEntradaId();
                UUID unidade = null;
                if (dto.unidadeId() != null) {
                    var u =
                            unidades.findById(dto.unidadeId())
                                    .orElseThrow(RegraNegocioException::naoEncontrado);
                    unidade = UUID.fromString(u.getCodigo());
                    Long daUnidade = pedidoDaUnidade(l, unidade);
                    conferirReferencia(entrada, daUnidade);
                    entrada = daUnidade;
                }
                conferirReferencia(codigo, unidade);
                if (pedido != null
                        && !pedido.equals(entrada)
                        && !pedido.equals(dto.pedidoSaidaId())) throw referenciaDivergente();
                if (entrada != null) conferirPedidoEntrada(l, entrada);
                if (dto.pedidoSaidaId() != null) {
                    var p =
                            saidas.findById(dto.pedidoSaidaId())
                                    .orElseThrow(RegraNegocioException::naoEncontrado);
                    contexto(l, p.getCliente().getId(), p.getArmazem().getId());
                }
                pedido = entrada;
                codigo = unidade;
            }
        }
        if (dados.get("pedidoId") != null)
            conferirReferencia(numero(dados.get("pedidoId")), pedido);
        if (dados.get("codigoUnidade") != null)
            conferirReferencia(codigoUnidade(dados.get("codigoUnidade")), codigo);
        if (dados.get("contagemId") != null)
            conferirReferencia(numero(dados.get("contagemId")), contagem);
        return new AlvoOperacional(pedido, codigo, contagem, entradaParaLock);
    }

    private Long pedidoDaUnidade(LinhaContingencia l, UUID codigo) {
        Long pedido =
                unidades.buscarPedidoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        conferirPedidoEntrada(l, pedido);
        return pedido;
    }

    private void conferirPedidoEntrada(LinhaContingencia l, Long id) {
        var p = entradas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        contexto(l, p.getCliente().getId(), p.getArmazem().getId());
    }

    private static UUID codigoUnidade(Object valor) {
        if (valor == null) return null;
        try {
            return UUID.fromString(valor.toString());
        } catch (IllegalArgumentException e) {
            throw CadastroSupport.invalido("Código de unidade inválido.");
        }
    }

    private static void conferirReferencia(Object informada, Object efetiva) {
        if (informada != null && !informada.equals(efetiva)) throw referenciaDivergente();
    }

    private static RegraNegocioException referenciaDivergente() {
        return CadastroSupport.invalido("Referências da contingência divergem do comando efetivo.");
    }

    public ContingenciaDto.Resultado consultar(@NotNull @Positive Long id) {
        acesso.exigirSupervisor();
        var l = linhas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(l);
        return resultado(l);
    }

    public PaginaResponse<ContingenciaDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        return listar(clienteId, armazemId, null, null, null, null, pagina, tamanho);
    }

    public PaginaResponse<ContingenciaDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            String identidade,
            ContingenciaDto.FiltroTipo tipo,
            ContingenciaDto.FiltroSituacao situacao,
            String pendencia,
            int pagina,
            int tamanho) {
        acesso.exigirSupervisor();
        autorizar(clienteId, armazemId);
        return PaginaResponse.de(
                linhas.consultar(
                        clienteId,
                        armazemId,
                        identidade == null ? null : CadastroSupport.codigo(identidade),
                        tipo == null ? null : TipoContingencia.valueOf(tipo.name()),
                        situacao == null ? null : SituacaoContingencia.valueOf(situacao.name()),
                        pendencia == null ? null : CadastroSupport.codigo(pendencia),
                        CadastroSupport.pagina(pagina, tamanho)),
                this::resultado);
    }

    private ContingenciaDto.Conteudo conteudo(LinhaContingencia l) {
        return mapper.readValue(l.getConteudoJson(), ContingenciaDto.Conteudo.class);
    }

    private ContingenciaDto.Resultado resultado(LinhaContingencia l) {
        return new ContingenciaDto.Resultado(
                l.getId(),
                l.getVersao(),
                l.getIdentidadeFato(),
                l.getCliente().getId(),
                l.getArmazem().getId(),
                l.getTipo(),
                l.getOcorridaEm(),
                l.getConteudoHash(),
                conteudo(l),
                l.getSituacao(),
                l.getPendencia(),
                l.getConciliadaEm(),
                l.getResultadoJson() == null
                        ? null
                        : mapper.readValue(l.getResultadoJson(), Object.class));
    }

    private void contexto(LinhaContingencia l, Long cliente, Long armazem) {
        if (!l.getCliente().getId().equals(cliente) || !l.getArmazem().getId().equals(armazem))
            throw conflito(
                    "CONTEXTO_CONTINGENCIA_DIVERGENTE",
                    "Resultado/dependência não pertence ao contexto da linha.");
        autorizar(cliente, armazem);
    }

    private void autorizar(LinhaContingencia l) {
        autorizar(l.getCliente().getId(), l.getArmazem().getId());
    }

    private void autorizar(Long cliente, Long armazem) {
        acesso.cliente(cliente);
        acesso.armazem(armazem);
    }

    private Map<String, Object> canonico(Map<String, Object> dados) {
        var out = new TreeMap<String, Object>();
        dados.forEach(
                (k, v) -> {
                    if (!"operacaoId".equals(k)) out.put(k, normalizar(v));
                });
        return out;
    }

    private Object normalizar(Object v) {
        if (v instanceof Map<?, ?> m) return canonico(mapa(m));
        if (v instanceof List<?> l) return l.stream().map(this::normalizar).toList();
        if (v instanceof Number n) {
            var numero = new BigDecimal(n.toString()).stripTrailingZeros();
            return numero.scale() < 0 ? numero.setScale(0) : numero;
        }
        return v;
    }

    private Map<String, Object> mapa(Object v) {
        if (!(v instanceof Map<?, ?>))
            throw CadastroSupport.invalido("Dados devem ser objeto estruturado.");
        return mapper.convertValue(v, new TypeReference<Map<String, Object>>() {});
    }

    private static Long numero(Object n) {
        try {
            return new BigDecimal(n.toString()).longValueExact();
        } catch (RuntimeException e) {
            throw CadastroSupport.invalido("Identificador numérico inválido.");
        }
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }
}
