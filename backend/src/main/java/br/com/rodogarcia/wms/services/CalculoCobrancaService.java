package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.AvariaEstoque;
import br.com.rodogarcia.wms.models.CalculoCobranca;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.MemoriaDiaria;
import br.com.rodogarcia.wms.models.MemoriaServico;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.CalculoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ItemTabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.MarcoFinanceiroAvariaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaDiariaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaServicoRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.RateioFatoServicoRepository;
import br.com.rodogarcia.wms.repositories.ServicoMinimoContratoRepository;
import br.com.rodogarcia.wms.repositories.TabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import br.com.rodogarcia.wms.repositories.VinculoTabelaClienteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Simulação local explicável; não aprova, fecha ciclo ou emite cobrança. */
@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class CalculoCobrancaService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CEM = new BigDecimal("100");
    private final CalculoCobrancaRepository calculos;
    private final ContratoCobrancaRepository contratos;
    private final ServicoMinimoContratoRepository minimos;
    private final VinculoTabelaClienteRepository vinculos;
    private final ItemTabelaCobrancaRepository itens;
    private final TabelaCobrancaRepository tabelas;
    private final FatoServicoRepository servicos;
    private final RateioFatoServicoRepository rateios;
    private final UnidadeLogisticaRepository unidades;
    private final ConteudoUnidadeRepository conteudos;
    private final BaixaSaidaRepository baixas;
    private final FatoPermanenciaRepository fatos;
    private final AvariaEstoqueRepository avarias;
    private final MarcoFinanceiroAvariaRepository marcos;
    private final MovimentoEstoqueRepository movimentos;
    private final MemoriaDiariaRepository diarias;
    private final MemoriaServicoRepository memoriasServico;
    private final ContextoCobrancaService contextos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final LinhaTemporalEstoqueService temporal;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;
    private final br.com.rodogarcia.wms.repositories.RevisaoContagemRepository contagens;
    private final ResolucaoFinanceiraCadastroService resolucaoCadastro;
    private final br.com.rodogarcia.wms.repositories.EntradaConferidaRepository entradas;

    public CalculoCobrancaService(
            CalculoCobrancaRepository calculos,
            ContratoCobrancaRepository contratos,
            ServicoMinimoContratoRepository minimos,
            VinculoTabelaClienteRepository vinculos,
            ItemTabelaCobrancaRepository itens,
            TabelaCobrancaRepository tabelas,
            FatoServicoRepository servicos,
            RateioFatoServicoRepository rateios,
            UnidadeLogisticaRepository unidades,
            ConteudoUnidadeRepository conteudos,
            BaixaSaidaRepository baixas,
            FatoPermanenciaRepository fatos,
            AvariaEstoqueRepository avarias,
            MarcoFinanceiroAvariaRepository marcos,
            MovimentoEstoqueRepository movimentos,
            MemoriaDiariaRepository diarias,
            MemoriaServicoRepository memoriasServico,
            ContextoCobrancaService contextos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            LinhaTemporalEstoqueService temporal,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock,
            br.com.rodogarcia.wms.repositories.RevisaoContagemRepository contagens,
            br.com.rodogarcia.wms.repositories.EntradaConferidaRepository entradas,
            ResolucaoFinanceiraCadastroService resolucaoCadastro) {
        this.resolucaoCadastro = resolucaoCadastro;
        this.calculos = calculos;
        this.contratos = contratos;
        this.minimos = minimos;
        this.vinculos = vinculos;
        this.itens = itens;
        this.tabelas = tabelas;
        this.servicos = servicos;
        this.rateios = rateios;
        this.unidades = unidades;
        this.conteudos = conteudos;
        this.baixas = baixas;
        this.fatos = fatos;
        this.avarias = avarias;
        this.marcos = marcos;
        this.movimentos = movimentos;
        this.diarias = diarias;
        this.memoriasServico = memoriasServico;
        this.contextos = contextos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.temporal = temporal;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
        this.contagens = contagens;
        this.entradas = entradas;
    }

    @Transactional
    public CalculoCobrancaDto.Resultado calcular(@NotNull @Valid CalculoCobrancaDto.Calcular d) {
        var ctx = contextos.bloquear(d.clienteId(), d.armazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        String hash = operacoes.hash("CALCULO", null, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, CalculoCobrancaDto.Resultado.class);
        if (replay != null) return replay;
        var apuracao = apurar(d);
        var c = apuracao.contrato();
        resolucaoCadastro.conferirPeriodo(
                d.resolucao(),
                d.periodoInicio(),
                d.periodoFim(),
                clock,
                c == null ? null : c.getFuso());
        var memoria = apuracao.memoria();
        var pendentes = apuracao.pendencias();
        var memoriaDiarias = memoria.diarias();
        var memoriaServicos = memoria.servicos();
        var subtotal = apuracao.subtotal();
        var minimo = apuracao.minimo();
        var gris = apuracao.gris();
        var total = apuracao.total();
        var entradasHash = apuracao.hash();
        var calc =
                calculos.saveAndFlush(
                        new CalculoCobranca(
                                ctx.cliente(),
                                ctx.armazem(),
                                c,
                                d.periodoInicio(),
                                d.periodoFim(),
                                c == null ? null : c.getFuso(),
                                c == null ? null : c.getMoeda(),
                                total == null ? "PENDENTE" : "COMPLETO",
                                1,
                                entradasHash,
                                mapper.writeValueAsString(memoria),
                                mapper.writeValueAsString(pendentes),
                                subtotal,
                                minimo,
                                gris,
                                total,
                                Instant.now(clock)
                                        .truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                                acesso.usuario()));
        for (var m : memoriaDiarias)
            diarias.save(
                    new MemoriaDiaria(
                            calc,
                            m.data(),
                            m.tabelaId() == null ? null : tabelas.getReferenceById(m.tabelaId()),
                            m.itemTabelaId() == null
                                    ? null
                                    : itens.getReferenceById(m.itemTabelaId()),
                            m.picoCobravel(),
                            m.equivalenciaSuspensa(),
                            m.valorEstoque(),
                            m.tarifa(),
                            m.valor(),
                            mapper.writeValueAsString(m)));
        for (var m : memoriaServicos)
            memoriasServico.save(
                    new MemoriaServico(
                            calc,
                            servicos.getReferenceById(m.fatoId()),
                            m.itemTabelaId() == null
                                    ? null
                                    : itens.getReferenceById(m.itemTabelaId()),
                            m.quantidade(),
                            m.preco(),
                            m.percentual(),
                            m.valorBase(),
                            m.valor(),
                            mapper.writeValueAsString(m.parcelas())));
        diarias.flush();
        memoriasServico.flush();
        var r = resposta(calc);
        auditoria.registrar(
                "CALCULO_COBRANCA",
                calc.getId(),
                "CALCULO",
                CadastroSupport.motivo(d.motivo()),
                null,
                resolucaoCadastro.paraAuditoria(
                        d.resolucao(), r, d.periodoInicio(), d.periodoFim()));
        operacoes.salvar(
                d.operacaoId(), "CALCULO", ctx.cliente(), ctx.armazem(), calc.getId(), hash, r);
        return r;
    }

    private record Apuracao(
            ContratoCobranca contrato,
            CalculoCobrancaDto.Memoria memoria,
            List<CalculoCobrancaDto.Pendencia> pendencias,
            BigDecimal subtotal,
            BigDecimal minimo,
            BigDecimal gris,
            BigDecimal total,
            String hash) {}

    private Apuracao apurar(CalculoCobrancaDto.Calcular d) {
        long duracao = ChronoUnit.DAYS.between(d.periodoInicio(), d.periodoFim());
        if (duracao < 1 || duracao > 366)
            throw CadastroSupport.invalido("Período [início,fim) entre 1 e 366 dias.");
        var pendentes = new ArrayList<CalculoCobrancaDto.Pendencia>();
        var cs =
                contratos.findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
                        d.clienteId(), d.armazemId());
        var c =
                cs.stream()
                        .filter(
                                x ->
                                        PeriodoCobrancaService.vigente(
                                                x.getVigenciaInicio(),
                                                x.getVigenciaFim(),
                                                d.periodoInicio()))
                        .findFirst()
                        .orElse(null);
        if (c == null)
            pendente(
                    pendentes,
                    "CONTRATO_AUSENTE",
                    null,
                    null,
                    null,
                    "Fuso, moeda, corte, mínimo e GRIS não configurados.");
        else if (c.getVigenciaFim() != null && d.periodoFim().isAfter(c.getVigenciaFim())) {
            pendente(
                    pendentes,
                    "CONTRATO_NAO_COBRE_PERIODO",
                    null,
                    null,
                    null,
                    "Calcule cada ciclo/configuração em seu período integral.");
            c = null;
        }
        var memoriaDiarias = new ArrayList<CalculoCobrancaDto.Diaria>();
        var memoriaServicos = new ArrayList<CalculoCobrancaDto.Servico>();
        var subtotalPorServico = new HashMap<Long, BigDecimal>();
        var contextoUnidades = unidades.historicoCobranca(d.clienteId(), d.armazemId());
        if (c != null) {
            ZoneId zona = ZoneId.of(c.getFuso());
            for (LocalDate dia = d.periodoInicio();
                    dia.isBefore(d.periodoFim());
                    dia = dia.plusDays(1)) {
                var diaria =
                        diaria(
                                d.clienteId(),
                                d.armazemId(),
                                dia,
                                zona,
                                contextoUnidades,
                                pendentes);
                memoriaDiarias.add(diaria);
                for (var regra : diaria.regras())
                    if (regra.servicoId() != null && regra.valor() != null)
                        subtotalPorServico.merge(regra.servicoId(), regra.valor(), BigDecimal::add);
            }
            for (var f :
                    servicos.findByClienteIdAndArmazemIdOrderByExecutadoEmAscIdAsc(
                            d.clienteId(), d.armazemId())) {
                LocalDate executada = f.getExecutadoEm().atZone(zona).toLocalDate();
                if (!f.getSituacao().equals("CONFIRMADO")
                        || executada.isBefore(d.periodoInicio())
                        || !executada.isBefore(d.periodoFim())) continue;
                var m = servico(d.clienteId(), d.armazemId(), f, executada, pendentes);
                memoriaServicos.add(m);
                if (m.valor() != null)
                    subtotalPorServico.merge(f.getServico().getId(), m.valor(), BigDecimal::add);
            }
        }
        BigDecimal subtotal =
                moeda(subtotalPorServico.values().stream().reduce(ZERO, BigDecimal::add));
        var ajustes = ajustes(c, d, memoriaDiarias, subtotalPorServico, pendentes);
        BigDecimal minimo = ajustes == null ? null : ajustes.minimoComplemento();
        BigDecimal gris = ajustes == null ? null : ajustes.valorGris();
        BigDecimal total =
                pendentes.isEmpty() && minimo != null && gris != null
                        ? moeda(subtotal.add(minimo).add(gris))
                        : null;
        var memoria =
                new CalculoCobrancaDto.Memoria(
                        List.copyOf(memoriaDiarias), List.copyOf(memoriaServicos), ajustes);
        String entradasHash =
                operacoes.hash(
                        "MEMORIA_CALCULO",
                        c == null ? null : c.getId(),
                        List.of(memoria, pendentes));
        return new Apuracao(
                c, memoria, List.copyOf(pendentes), subtotal, minimo, gris, total, entradasHash);
    }

    /** Same live sources under the caller's context locks; creates no calculation or audit. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void conferirAtual(CalculoCobranca calc) {
        if (!atual(calc))
            throw RegraNegocioException.conflito(
                    "CALCULO_DESATUALIZADO",
                    "Fatos ou configuracao mudaram; gere novo calculo e nova versao.");
    }

    /** Consulta pura para registrar uma referência externa existente como conflito. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public boolean atual(CalculoCobranca calc) {
        var a =
                apurar(
                        new CalculoCobrancaDto.Calcular(
                                java.util.UUID.randomUUID(),
                                calc.getCliente().getId(),
                                calc.getArmazem().getId(),
                                calc.getPeriodoInicio(),
                                calc.getPeriodoFim(),
                                "Conferencia integral do fechamento",
                                null));
        return calc.getEntradasHash().equals(a.hash());
    }

    private TabelaCobranca tabela(
            Long cliente, Long armazem, LocalDate data, List<CalculoCobrancaDto.Pendencia> p) {
        var vs =
                vinculos
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(cliente, armazem)
                        .stream()
                        .filter(
                                v ->
                                        PeriodoCobrancaService.vigente(
                                                v.getVigenciaInicio(), v.getVigenciaFim(), data))
                        .toList();
        if (vs.size() != 1) {
            pendente(
                    p,
                    vs.isEmpty() ? "TABELA_AUSENTE" : "TABELA_AMBIGUA",
                    data,
                    null,
                    null,
                    "Vínculo explícito único por data é obrigatório.");
            return null;
        }
        var t = vs.getFirst().getTabela();
        if (!PeriodoCobrancaService.vigente(t.getVigenciaInicio(), t.getVigenciaFim(), data)) {
            pendente(p, "TABELA_FORA_VIGENCIA", data, null, null, "Tabela não cobre esta data.");
            return null;
        }
        return t;
    }

    private ItemTabelaCobranca itemServico(TabelaCobranca t, Long servico, String categoria) {
        if (t == null) return null;
        return itens.findByTabelaIdOrderByIdAsc(t.getId()).stream()
                .filter(
                        i ->
                                i.getServico().getId().equals(servico)
                                        && i.getCategoria().equals(categoria))
                .findFirst()
                .orElse(null);
    }

    private ItemTabelaCobranca itemArmazenagem(
            TabelaCobranca t,
            String categoria,
            LocalDate data,
            List<CalculoCobrancaDto.Pendencia> p) {
        if (t == null) return null;
        var candidatos =
                itens.findByTabelaIdOrderByIdAsc(t.getId()).stream()
                        .filter(
                                i ->
                                        i.getServico().getTipo().equals("ARMAZENAGEM")
                                                && (i.getCategoria().equals(categoria)
                                                        || i.getCategoria().isEmpty()))
                        .toList();
        var porServico = new LinkedHashMap<Long, ItemTabelaCobranca>();
        for (var i : candidatos) {
            var anterior = porServico.get(i.getServico().getId());
            if (anterior == null || i.getCategoria().equals(categoria))
                porServico.put(i.getServico().getId(), i);
        }
        if (porServico.size() != 1) {
            pendente(
                    p,
                    porServico.isEmpty()
                            ? "TARIFA_ARMAZENAGEM_AUSENTE"
                            : "REGRA_ARMAZENAGEM_AMBIGUA",
                    data,
                    null,
                    null,
                    "Categoria "
                            + categoria
                            + " precisa de um único serviço de armazenagem aplicável.");
            return null;
        }
        return porServico.values().iterator().next();
    }

    private record Historico(
            UnidadeLogistica unidade,
            List<FatoPermanencia> fatos,
            List<AvariaEstoque> avarias,
            List<AvariaLegada> avariasLegadas,
            Instant retirada) {}

    /** O UUID do movimento identifica o ciclo legado, sem inferir dano pela flag atual. */
    private record AvariaLegada(
            Long movimentoId, String operacaoId, Instant marcadaEm, AvariaEstoque cobertura) {}

    private List<AvariaLegada> avariasLegadas(UnidadeLogistica u, List<AvariaEstoque> as) {
        return avariasLegadas(u, as, movimentos.findByUnidadeIdOrderByInstanteAscIdAsc(u.getId()));
    }

    private List<AvariaLegada> avariasLegadas(
            UnidadeLogistica u,
            List<AvariaEstoque> as,
            List<br.com.rodogarcia.wms.models.MovimentoEstoque> historico) {
        var legadas = new ArrayList<AvariaLegada>();
        for (int n = 0; n < historico.size(); n++) {
            var m = historico.get(n);
            if (!m.getAcao().equals("AVARIA_ESTOQUE")) continue;
            AvariaEstoque cobertura = null;
            for (int i = n + 1; i < historico.size(); i++) {
                var proximo = historico.get(i);
                if (proximo.getAcao().equals("AVARIA_ESTOQUE")) break;
                if (!proximo.getAcao().equals("AVARIA_DETALHADA")) continue;
                try {
                    var antes = mapper.readValue(proximo.getDadosAntes(), EstoqueDto.Unidade.class);
                    var depois =
                            mapper.readValue(proximo.getResultado(), EstoqueDto.Confirmacao.class);
                    if (antes.avariaPosterior() && depois.avaria() != null) {
                        var snapshot = depois.avaria();
                        cobertura =
                                as.stream()
                                        .filter(
                                                a ->
                                                        a.getId().equals(snapshot.id())
                                                                && a.getCicloId()
                                                                        .equals(snapshot.cicloId())
                                                                && a.getUnidade()
                                                                        .getId()
                                                                        .equals(u.getId())
                                                                && !a.getRegistradaEm()
                                                                        .isBefore(m.getInstante()))
                                        .findFirst()
                                        .orElse(null);
                    }
                } catch (RuntimeException e) {
                    // Histórico incompatível não comprova cobertura; mantém pendência explícita.
                }
                break;
            }
            legadas.add(new AvariaLegada(m.getId(), m.getOperacaoId(), m.getInstante(), cobertura));
        }
        return List.copyOf(legadas);
    }

    private boolean avariaLegadaPendente(
            Historico h, Instant instante, LocalDate dia, List<CalculoCobrancaDto.Pendencia> p) {
        boolean pendente = false;
        for (var l : h.avariasLegadas()) {
            if (instante.isBefore(l.marcadaEm())) continue;
            var a = l.cobertura();
            // Registro posterior só comprova o intervalo que a ocorrência detalhada declara.
            if (a != null && !instante.isBefore(a.getOcorridaEm())) {
                if (a.getReconhecidaEm() == null
                        && (a.getResolvidaEm() == null || instante.isBefore(a.getResolvidaEm()))) {
                    pendente(
                            p,
                            "RESPONSABILIDADE_AVARIA_PENDENTE",
                            dia,
                            h.unidade().getId(),
                            l.movimentoId(),
                            "movimento_estoque="
                                    + l.movimentoId()
                                    + "; cicloBE08="
                                    + l.operacaoId()
                                    + "; cicloDetalhado="
                                    + a.getCicloId()
                                    + "; avaria="
                                    + a.getId()
                                    + ". Gestor deve reconhecer o intervalo detalhado, inclusive valor físico sem diária.");
                }
                continue;
            }
            pendente = true;
            pendente(
                    p,
                    "AVARIA_BE08_SEM_DETALHE",
                    dia,
                    h.unidade().getId(),
                    l.movimentoId(),
                    "movimento_estoque="
                            + l.movimentoId()
                            + "; cicloBE08="
                            + l.operacaoId()
                            + "; marcadaEm="
                            + l.marcadaEm()
                            + (a == null
                                    ? "; sem ocorrência detalhada vinculada."
                                    : "; cicloDetalhado="
                                            + a.getCicloId()
                                            + "; inicioComprovado="
                                            + a.getOcorridaEm()
                                            + "; lacuna anterior preservada.")
                            + " Quantidade/período/responsabilidade sem comprovação; sem estimativa financeira.");
        }
        if (h.unidade().isAvariaPosterior()
                && h.avariasLegadas().isEmpty()
                && h.avarias().stream().noneMatch(a -> a.getResolvidaEm() == null)) {
            pendente = true;
            pendente(
                    p,
                    "HISTORICO_AVARIA_INSUFICIENTE",
                    dia,
                    h.unidade().getId(),
                    null,
                    "Marcador de avaria sem movimento/ocorrência datados; flag atual não comprova período anterior.");
        }
        return pendente;
    }

    private static class PicoRegra {
        ItemTabelaCobranca item;
        String categoria;
        BigDecimal pico = ZERO;
        Instant instante;

        PicoRegra(ItemTabelaCobranca item, String categoria) {
            this.item = item;
            this.categoria = categoria;
        }
    }

    private CalculoCobrancaDto.Diaria diaria(
            Long cliente,
            Long armazem,
            LocalDate dia,
            ZoneId zona,
            List<UnidadeLogistica> us,
            List<CalculoCobrancaDto.Pendencia> p) {
        Instant inicio = dia.atStartOfDay(zona).toInstant();
        Instant fim = dia.plusDays(1).atStartOfDay(zona).toInstant();
        var historicos = new ArrayList<Historico>();
        var pontos = new TreeSet<Instant>();
        pontos.add(inicio);
        pontos.add(fim);
        for (var u : us) {
            if (u.getInicioArmazenagemEm() == null
                    || u.getInicioArmazenagemEm().atZone(zona).toLocalDate().isAfter(dia)) continue;
            var fs = fatos.findByUnidadeIdOrderByOcorridaEmAscIdAsc(u.getId());
            Instant retirada =
                    fs.stream()
                            .filter(
                                    f ->
                                            (f.getTipo().equals("RETIRADA")
                                                            || f.getTipo().equals("AJUSTE_ESTOQUE")
                                                                    && f.getEquivalenciaDepois()
                                                                                    .signum()
                                                                            == 0)
                                                    && f.getQuantidadeDepois().signum() == 0)
                            .map(FatoPermanencia::getOcorridaEm)
                            .findFirst()
                            .orElse(null);
            if (retirada != null && !dia.isBefore(retirada.atZone(zona).toLocalDate())) continue;
            if (!u.isAtiva() && retirada == null) {
                pendente(
                        p,
                        "FIM_PERMANENCIA_INSUFICIENTE",
                        dia,
                        u.getId(),
                        null,
                        "Unidade encerrada sem retirada ou ajuste a zero datados comprovados.");
                continue;
            }
            var as = avarias.findByUnidadeIdOrderById(u.getId());
            var legadas = avariasLegadas(u, as);
            historicos.add(new Historico(u, fs, as, legadas, retirada));
            ponto(pontos, u.getInicioArmazenagemEm(), inicio, fim);
            for (var l : legadas) ponto(pontos, l.marcadaEm(), inicio, fim);
            for (var f : fs) ponto(pontos, f.getOcorridaEm(), inicio, fim);
            for (var a : as) {
                ponto(pontos, a.getOcorridaEm(), inicio, fim);
                ponto(pontos, a.getResolvidaEm(), inicio, fim);
            }
        }
        var t = historicos.isEmpty() ? null : tabela(cliente, armazem, dia, p);
        var itensCategoria = new HashMap<String, ItemTabelaCobranca>();
        var regras = new LinkedHashMap<String, PicoRegra>();
        for (var h : historicos) {
            String categoria = h.unidade().getTipo().name();
            if (!itensCategoria.containsKey(categoria))
                itensCategoria.put(categoria, itemArmazenagem(t, categoria, dia, p));
            var i = itensCategoria.get(categoria);
            String chave = i == null ? "AUSENTE:" + categoria : "ITEM:" + i.getId();
            regras.putIfAbsent(chave, new PicoRegra(i, i == null ? categoria : i.getCategoria()));
        }
        BigDecimal pico = ZERO, suspensa = ZERO;
        var segmentos = new ArrayList<CalculoCobrancaDto.Segmento>();
        var ps = new ArrayList<>(pontos);
        for (int n = 0; n < ps.size() - 1; n++) {
            Instant instante = ps.get(n);
            var contribuicoes = new ArrayList<CalculoCobrancaDto.Contribuicao>();
            var somaRegra = new HashMap<String, BigDecimal>();
            BigDecimal soma = ZERO, somaSuspensa = ZERO;
            for (var h : historicos) {
                if (instante.isBefore(h.unidade().getInicioArmazenagemEm())) continue;
                var c = contribuicao(h, instante, dia, p);
                contribuicoes.add(c);
                if (c.equivalenciaLiquida() != null) {
                    soma = soma.add(c.equivalenciaLiquida());
                    somaSuspensa = somaSuspensa.add(c.equivalenciaSuspensa());
                    var i = itensCategoria.get(c.categoria());
                    String chave = i == null ? "AUSENTE:" + c.categoria() : "ITEM:" + i.getId();
                    somaRegra.merge(chave, c.equivalenciaLiquida(), BigDecimal::add);
                }
            }
            for (var e : somaRegra.entrySet()) {
                var regra = regras.get(e.getKey());
                if (e.getValue().compareTo(regra.pico) > 0) {
                    regra.pico = e.getValue();
                    regra.instante = instante;
                }
            }
            pico = pico.max(soma);
            suspensa = suspensa.max(somaSuspensa);
            segmentos.add(
                    new CalculoCobrancaDto.Segmento(
                            instante, ps.get(n + 1), List.copyOf(contribuicoes)));
        }
        var rs = new ArrayList<CalculoCobrancaDto.RegraDiaria>();
        BigDecimal valor = ZERO;
        boolean tarifaCompleta = true;
        for (var regra : regras.values()) {
            var i = regra.item;
            BigDecimal v = i == null ? null : moeda(regra.pico.multiply(i.getPreco()));
            if (v == null) tarifaCompleta = false;
            else valor = valor.add(v);
            rs.add(
                    new CalculoCobrancaDto.RegraDiaria(
                            i == null ? null : i.getTabela().getId(),
                            i == null ? null : i.getId(),
                            i == null ? null : i.getServico().getId(),
                            regra.categoria,
                            regra.pico,
                            i == null ? null : i.getPreco(),
                            v,
                            regra.instante));
        }
        var unica = rs.size() == 1 ? rs.getFirst() : null;
        var valorFisico = valorFisicoDoDia(cliente, armazem, dia, zona, us, p);
        return new CalculoCobrancaDto.Diaria(
                dia,
                t == null ? null : t.getId(),
                unica == null ? null : unica.itemTabelaId(),
                pico,
                suspensa,
                valorFisico.pico(),
                unica == null ? null : unica.tarifa(),
                tarifaCompleta ? moeda(valor) : null,
                List.copyOf(rs),
                List.copyOf(segmentos),
                valorFisico.intervalos());
    }

    private CalculoCobrancaDto.Contribuicao contribuicao(
            Historico h, Instant instante, LocalDate dia, List<CalculoCobrancaDto.Pendencia> p) {
        return contribuicao(h, instante, dia, p, false);
    }

    private CalculoCobrancaDto.Contribuicao contribuicao(
            Historico h,
            Instant instante,
            LocalDate dia,
            List<CalculoCobrancaDto.Pendencia> p,
            boolean fisico) {
        return contribuicao(h, instante, dia, p, fisico, null);
    }

    private CalculoCobrancaDto.Contribuicao contribuicao(
            Historico h,
            Instant instante,
            LocalDate dia,
            List<CalculoCobrancaDto.Pendencia> p,
            boolean fisico,
            DadosIndicador dados) {
        var u = h.unidade();
        LinhaTemporalEstoqueService.Base base;
        try {
            base =
                    dados != null
                            ? temporal.fisicoNoInstante(
                                    u,
                                    instante,
                                    h.fatos(),
                                    dados.transformacoes()
                                            .getOrDefault(u.getPedido().getId(), List.of()))
                            : fisico
                                    ? temporal.fisicoNoInstante(u, instante)
                                    : temporal.noInstante(u, instante);
        } catch (RegraNegocioException e) {
            pendente(p, "HISTORICO_QUANTIDADE_INSUFICIENTE", dia, u.getId(), null, e.getMessage());
            return new CalculoCobrancaDto.Contribuicao(
                    u.getId(),
                    u.getCodigo(),
                    u.getTipo().name(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(),
                    h.fatos().stream().map(FatoPermanencia::getId).toList(),
                    List.of());
        }
        if (base.quantidade().signum() == 0)
            return new CalculoCobrancaDto.Contribuicao(
                    u.getId(),
                    u.getCodigo(),
                    u.getTipo().name(),
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    List.of(),
                    h.fatos().stream().map(FatoPermanencia::getId).toList(),
                    List.of());
        boolean legadoPendente = avariaLegadaPendente(h, instante, dia, p);
        BigDecimal danificada = ZERO, responsavel = ZERO;
        boolean ambiguo = legadoPendente;
        boolean responsabilidadePendente = false;
        var ids = new ArrayList<Long>();
        for (var a : h.avarias()) {
            if (instante.isBefore(a.getOcorridaEm())
                    || a.getResolvidaEm() != null && !instante.isBefore(a.getResolvidaEm()))
                continue;
            ids.add(a.getId());
            BigDecimal afetada = a.getQuantidade();
            for (var f : h.fatos()) {
                if (!List.of("RETIRADA", "AJUSTE_ESTOQUE").contains(f.getTipo())
                        || f.getOcorridaEm().isBefore(a.getOcorridaEm())
                        || f.getOcorridaEm().isAfter(instante)) continue;
                if (f.getQuantidadeAntes().compareTo(f.getQuantidadeDepois()) == 0) continue;
                var marco =
                        (dados == null
                                        ? marcos.findByAvariaIdOrderByIdAsc(a.getId())
                                        : dados.marcos().getOrDefault(a.getId(), List.of()))
                                .stream()
                                        .filter(
                                                m ->
                                                        m.getFatoPermanencia()
                                                                .getId()
                                                                .equals(f.getId()))
                                        .findFirst()
                                        .orElse(null);
                if (marco == null) {
                    ambiguo = true;
                    pendente(
                            p,
                            "DESTINO_AVARIA_INSUFICIENTE",
                            dia,
                            u.getId(),
                            f.getId(),
                            "Avaria "
                                    + a.getId()
                                    + ": Gestor deve identificar a parte afetada remanescente neste evento.");
                } else afetada = marco.getQuantidadeAfetada();
            }
            danificada = danificada.add(afetada);
            if (a.getReconhecidaEm() == null) {
                responsabilidadePendente = true;
                if (!fisico)
                    pendente(
                            p,
                            "RESPONSABILIDADE_AVARIA_PENDENTE",
                            dia,
                            u.getId(),
                            a.getId(),
                            "Reconhecimento financeiro por Gestor necessário.");
            } else if (a.getResponsabilidade().equals("RODOGARCIA"))
                responsavel = responsavel.add(afetada);
        }
        if (u.getCondicao() == CondicaoMercadoria.AVARIADA
                && ids.isEmpty()
                && h.avarias().stream()
                        .noneMatch(
                                a ->
                                        a.getResolvidaEm() != null
                                                && !a.getResolvidaEm().isAfter(instante))) {
            responsabilidadePendente = true;
            danificada = base.quantidade();
            if (!fisico)
                pendente(
                        p,
                        "AVARIA_INICIAL_SEM_RECONHECIMENTO",
                        dia,
                        u.getId(),
                        null,
                        "Condição inicial preservada; falta período/responsabilidade comprovados.");
        }
        if (danificada.compareTo(base.quantidade()) > 0) {
            ambiguo = true;
            pendente(
                    p,
                    "AVARIAS_HISTORICAS_INCONSISTENTES",
                    dia,
                    u.getId(),
                    null,
                    "Agregado histórico "
                            + danificada.toPlainString()
                            + " excede base "
                            + base.quantidade().toPlainString()
                            + "; não há clamp nem presunção de partes disjuntas.");
        }
        BigDecimal suspensa =
                ambiguo || responsabilidadePendente
                        ? null
                        : base.equivalencia()
                                .multiply(responsavel)
                                .divide(base.quantidade(), 6, RoundingMode.HALF_UP);
        BigDecimal liquida = suspensa == null ? null : base.equivalencia().subtract(suspensa);
        // A contribuição e sua explicação usam a mesma origem no mesmo instante/transação.
        var quantidadesOrigem =
                new LinkedHashMap<br.com.rodogarcia.wms.models.ConteudoUnidade, BigDecimal>();
        for (var c :
                dados == null
                        ? conteudos.buscarOrigens(u.getId())
                        : dados.origens().getOrDefault(u.getId(), List.of()))
            quantidadesOrigem.put(
                    c,
                    dados == null
                            ? quantidadeOrigemNoInstante(u, c, instante)
                            : quantidadeOrigemNoInstante(
                                    c,
                                    instante,
                                    dados.revisoes().getOrDefault(u.getId(), List.of()),
                                    dados.baixas().getOrDefault(u.getId(), List.of())));
        BigDecimal valor =
                ambiguo
                        ? null
                        : valorEstoque(u, base.quantidade(), danificada, dia, p, quantidadesOrigem);
        return new CalculoCobrancaDto.Contribuicao(
                u.getId(),
                u.getCodigo(),
                u.getTipo().name(),
                base.quantidade(),
                base.equivalencia(),
                legadoPendente ? null : danificada,
                suspensa,
                liquida,
                valor,
                List.copyOf(ids),
                h.fatos().stream()
                        .filter(f -> !f.getOcorridaEm().isAfter(instante))
                        .map(FatoPermanencia::getId)
                        .toList(),
                origensValor(quantidadesOrigem));
    }

    private BigDecimal valorEstoque(
            UnidadeLogistica u,
            BigDecimal base,
            BigDecimal avariada,
            LocalDate dia,
            List<CalculoCobrancaDto.Pendencia> p,
            Map<br.com.rodogarcia.wms.models.ConteudoUnidade, BigDecimal> quantidadesOrigem) {
        BigDecimal soma = ZERO, quantidade = ZERO;
        boolean completo = true;
        br.com.rodogarcia.wms.models.ItemNotaEntrada referenciaPreco = null;
        for (var origem : quantidadesOrigem.entrySet()) {
            var c = origem.getKey();
            BigDecimal q = origem.getValue();
            if (q.signum() == 0) continue;
            quantidade = quantidade.add(q);
            var i = c.getEntrada().getItemChegada().getItemNota();
            if (i.getValorMercadoria() == null || i.getQuantidadePrevista().signum() <= 0) {
                completo = false;
                pendente(
                        p,
                        "VALOR_NOTA_AUSENTE",
                        dia,
                        u.getId(),
                        i.getId(),
                        "Valor fixo do item de origem necessário para indicador/GRIS.");
            } else {
                if (avariada.signum() > 0
                        && referenciaPreco != null
                        && referenciaPreco
                                        .getValorMercadoria()
                                        .multiply(i.getQuantidadePrevista())
                                        .compareTo(
                                                i.getValorMercadoria()
                                                        .multiply(
                                                                referenciaPreco
                                                                        .getQuantidadePrevista()))
                                != 0) {
                    completo = false;
                    pendente(
                            p,
                            "ORIGEM_AVARIA_VALOR_INSUFICIENTE",
                            dia,
                            u.getId(),
                            null,
                            "Preços unitários das origens diferem; a parte danificada não foi identificada por origem. Sem desconto uniforme presumido.");
                }
                if (referenciaPreco == null) referenciaPreco = i;
                soma =
                        soma.add(
                                i.getValorMercadoria()
                                        .multiply(q)
                                        .divide(
                                                i.getQuantidadePrevista(),
                                                12,
                                                RoundingMode.HALF_UP));
            }
        }
        if (quantidade.compareTo(base) != 0) {
            completo = false;
            pendente(
                    p,
                    "ORIGEM_VALOR_HISTORICA_INSUFICIENTE",
                    dia,
                    u.getId(),
                    null,
                    "Quantidade das origens datadas não reconcilia a base da unidade.");
        }
        return completo
                ? soma.multiply(base.subtract(avariada)).divide(base, 12, RoundingMode.HALF_UP)
                : null;
    }

    private List<CalculoCobrancaDto.OrigemValor> origensValor(
            Map<br.com.rodogarcia.wms.models.ConteudoUnidade, BigDecimal> quantidadesOrigem) {
        var origens = new ArrayList<CalculoCobrancaDto.OrigemValor>();
        for (var origem : quantidadesOrigem.entrySet()) {
            var c = origem.getKey();
            var q = origem.getValue();
            if (q.signum() == 0) continue;
            var i = c.getEntrada().getItemChegada().getItemNota();
            var preco =
                    i.getValorMercadoria() == null || i.getQuantidadePrevista().signum() == 0
                            ? null
                            : i.getValorMercadoria()
                                    .divide(i.getQuantidadePrevista(), 12, RoundingMode.HALF_UP);
            origens.add(
                    new CalculoCobrancaDto.OrigemValor(
                            c.getEntrada().getId(),
                            i.getNota().getId(),
                            i.getId(),
                            q,
                            i.getQuantidadePrevista(),
                            i.getValorMercadoria(),
                            preco,
                            preco == null ? null : preco.multiply(q)));
        }
        return List.copyOf(origens);
    }

    private record ValorDoDia(BigDecimal pico, List<CalculoCobrancaDto.SegmentoValor> intervalos) {}

    private BigDecimal quantidadeOrigemNoInstante(
            UnidadeLogistica u, br.com.rodogarcia.wms.models.ConteudoUnidade c, Instant instante) {
        return quantidadeOrigemNoInstante(
                c,
                instante,
                contagens.findByContagemUnidadeIdAndSituacao(
                        u.getId(), br.com.rodogarcia.wms.models.SituacaoRevisaoContagem.APLICADA),
                baixas.buscarDaUnidade(u.getId()));
    }

    private BigDecimal quantidadeOrigemNoInstante(
            br.com.rodogarcia.wms.models.ConteudoUnidade c,
            Instant instante,
            List<br.com.rodogarcia.wms.models.RevisaoContagem> revisoes,
            List<br.com.rodogarcia.wms.models.BaixaSaida> retiradas) {
        BigDecimal q = c.getQuantidade();
        for (var r : revisoes) {
            if (r.getAplicadaEm() == null || !r.getAplicadaEm().isAfter(instante)) continue;
            for (var delta : mapper.readTree(r.getEfeitoJson()).get("origens"))
                if (delta.get("entradaId").longValue() == c.getEntrada().getId())
                    q = q.add(new BigDecimal(delta.get("quantidade").asString()));
        }
        for (var b : retiradas)
            if (b.getEntradaOrigem().getId().equals(c.getEntrada().getId())
                    && b.getRetirada().getRetiradaEm().isAfter(instante))
                q = q.add(b.getQuantidade());
        return q;
    }

    public record ValorIndicador(
            CalculoCobrancaDto.Contribuicao unidade,
            List<CalculoCobrancaDto.Pendencia> pendencias) {}

    @Transactional(readOnly = true)
    public ValorIndicador indicadorUnidade(UnidadeLogistica u, Instant instante, ZoneId zona) {
        return indicadoresUnidades(List.of(u), instante, zona).getFirst();
    }

    /**
     * Históricos completos por IDs já autorizados; cada lote limita binds, sem cache compartilhado.
     */
    @Transactional(readOnly = true)
    public List<ValorIndicador> indicadoresUnidades(
            List<UnidadeLogistica> selecionadas, Instant instante, ZoneId zona) {
        // Preservar todas as revalidações por unidade, antes da primeira leitura dos históricos.
        for (var u : selecionadas) {
            acesso.exigirSupervisor();
            acesso.cliente(u.getPedido().getCliente().getId());
            acesso.armazem(u.getPedido().getArmazem().getId());
        }
        var resultado = new ArrayList<ValorIndicador>();
        var unicas = new LinkedHashMap<Long, UnidadeLogistica>();
        selecionadas.forEach(u -> unicas.putIfAbsent(u.getId(), u));
        var lista = List.copyOf(unicas.values());
        var valores = new HashMap<Long, ValorIndicador>();
        for (int inicio = 0; inicio < lista.size(); inicio += 500) {
            var lote = lista.subList(inicio, Math.min(inicio + 500, lista.size()));
            var ids = lote.stream().map(UnidadeLogistica::getId).toList();
            var fs =
                    fatos.buscarParaIndicador(ids).stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            f -> f.getUnidade().getId()));
            var av =
                    avarias.buscarParaIndicador(ids).stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            a -> a.getUnidade().getId()));
            var ms = new HashMap<Long, List<br.com.rodogarcia.wms.models.MovimentoEstoque>>();
            for (var m : movimentos.buscarParaIndicador(ids))
                ms.computeIfAbsent(m.getUnidadeId(), k -> new ArrayList<>()).add(m.getMovimento());
            var os =
                    conteudos.buscarParaIndicador(ids).stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            c -> c.getUnidade().getId()));
            var rs =
                    contagens.buscarParaIndicador(ids).stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            c -> c.getContagem().getUnidade().getId()));
            var bs =
                    baixas.buscarParaIndicador(ids).stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            b -> b.getReserva().getUnidade().getId()));
            var mar =
                    marcos.buscarParaIndicador(ids).stream()
                            .collect(
                                    java.util.stream.Collectors.groupingBy(
                                            m -> m.getAvaria().getId()));
            var ts =
                    temporal.transformacoesParaIndicador(
                            lote.stream().map(u -> u.getPedido().getId()).distinct().toList());
            var dados = new DadosIndicador(os, rs, bs, mar, ts);
            for (var u : lote) {
                var as = av.getOrDefault(u.getId(), List.of());
                var h =
                        new Historico(
                                u,
                                fs.getOrDefault(u.getId(), List.of()),
                                as,
                                avariasLegadas(u, as, ms.getOrDefault(u.getId(), List.of())),
                                null);
                var p = new ArrayList<CalculoCobrancaDto.Pendencia>();
                valores.put(
                        u.getId(),
                        new ValorIndicador(
                                contribuicao(
                                        h,
                                        instante,
                                        instante.atZone(zona).toLocalDate(),
                                        p,
                                        true,
                                        dados),
                                List.copyOf(p)));
            }
        }
        selecionadas.forEach(u -> resultado.add(valores.get(u.getId())));
        return List.copyOf(resultado);
    }

    private record DadosIndicador(
            Map<Long, List<br.com.rodogarcia.wms.models.ConteudoUnidade>> origens,
            Map<Long, List<br.com.rodogarcia.wms.models.RevisaoContagem>> revisoes,
            Map<Long, List<br.com.rodogarcia.wms.models.BaixaSaida>> baixas,
            Map<Long, List<br.com.rodogarcia.wms.models.MarcoFinanceiroAvaria>> marcos,
            Map<Long, List<br.com.rodogarcia.wms.models.OperacaoUnidade>> transformacoes) {}

    private ValorDoDia valorFisicoDoDia(
            Long cliente,
            Long armazem,
            LocalDate dia,
            ZoneId zona,
            List<UnidadeLogistica> us,
            List<CalculoCobrancaDto.Pendencia> p) {
        Instant inicio = dia.atStartOfDay(zona).toInstant();
        Instant fim = dia.plusDays(1).atStartOfDay(zona).toInstant();
        var pontos = new TreeSet<Instant>();
        pontos.add(inicio);
        pontos.add(fim);
        var historicos = new ArrayList<Historico>();
        for (var u : us) {
            if (!u.getCriadaEm().isBefore(fim)) continue;
            var fs = fatos.findByUnidadeIdOrderByOcorridaEmAscIdAsc(u.getId());
            var as = avarias.findByUnidadeIdOrderById(u.getId());
            var legadas = avariasLegadas(u, as);
            historicos.add(new Historico(u, fs, as, legadas, null));
            ponto(pontos, u.getCriadaEm(), inicio, fim);
            for (var l : legadas) ponto(pontos, l.marcadaEm(), inicio, fim);
            for (var f : fs) ponto(pontos, f.getOcorridaEm(), inicio, fim);
            for (var a : as) {
                ponto(pontos, a.getOcorridaEm(), inicio, fim);
                ponto(pontos, a.getResolvidaEm(), inicio, fim);
            }
            for (var c : conteudos.buscarOrigens(u.getId()))
                ponto(pontos, c.getEntrada().getUnitizadaEm(), inicio, fim);
        }
        var es = entradas.historicoCobranca(cliente, armazem);
        for (var e : es) {
            ponto(pontos, e.getItemChegada().getChegada().getChegouEm(), inicio, fim);
            ponto(pontos, e.getUnitizadaEm(), inicio, fim);
        }
        var instantes = new ArrayList<>(pontos);
        var intervalos = new ArrayList<CalculoCobrancaDto.SegmentoValor>();
        BigDecimal pico = ZERO;
        boolean completo = true;
        for (int n = 0; n < instantes.size() - 1; n++) {
            Instant instante = instantes.get(n);
            var valores = new ArrayList<CalculoCobrancaDto.Contribuicao>();
            var valoresEntradas = new ArrayList<CalculoCobrancaDto.ValorEntrada>();
            BigDecimal soma = ZERO;
            boolean segmentoCompleto = true;
            for (var h : historicos) {
                if (instante.isBefore(h.unidade().getCriadaEm())) continue;
                var origem = conteudos.buscarOrigens(h.unidade().getId());
                if (origem.stream().anyMatch(c -> c.getEntrada().getUnitizadaEm() == null)) {
                    pendente(
                            p,
                            "UNITIZACAO_SEM_MARCO",
                            dia,
                            h.unidade().getId(),
                            null,
                            "Origem unitizada sem data comprovada; não somar unidade e entrada como estoques distintos.");
                    segmentoCompleto = false;
                    continue;
                }
                if (origem.stream()
                        .anyMatch(c -> instante.isBefore(c.getEntrada().getUnitizadaEm())))
                    continue;
                var valor = contribuicao(h, instante, dia, p, true);
                valores.add(valor);
                if (valor.valorEstoque() == null) segmentoCompleto = false;
                else soma = soma.add(valor.valorEstoque());
            }
            for (var e : es) {
                if (instante.isBefore(e.getItemChegada().getChegada().getChegouEm())
                        || e.getUnitizadaEm() != null && !instante.isBefore(e.getUnitizadaEm()))
                    continue;
                var i = e.getItemChegada().getItemNota();
                BigDecimal valor =
                        i.getValorMercadoria() == null || i.getQuantidadePrevista().signum() == 0
                                ? null
                                : i.getValorMercadoria()
                                        .multiply(e.getQuantidadeTriagem())
                                        .divide(
                                                i.getQuantidadePrevista(),
                                                12,
                                                RoundingMode.HALF_UP);
                if (valor == null) {
                    segmentoCompleto = false;
                    pendente(
                            p,
                            "VALOR_NOTA_AUSENTE",
                            dia,
                            null,
                            i.getId(),
                            "Valor da entrada física não unitizada necessário para indicador/GRIS.");
                } else soma = soma.add(valor);
                valoresEntradas.add(
                        new CalculoCobrancaDto.ValorEntrada(
                                e.getId(),
                                i.getNota().getId(),
                                i.getId(),
                                e.getQuantidadeTriagem(),
                                e.getQuantidadeQuarentena(),
                                valor));
            }
            if (!segmentoCompleto) completo = false;
            else pico = pico.max(soma);
            intervalos.add(
                    new CalculoCobrancaDto.SegmentoValor(
                            instante,
                            instantes.get(n + 1),
                            segmentoCompleto ? moeda(soma) : null,
                            List.copyOf(valores),
                            List.copyOf(valoresEntradas)));
        }
        return new ValorDoDia(completo ? moeda(pico) : null, List.copyOf(intervalos));
    }

    private CalculoCobrancaDto.Servico servico(
            Long cliente,
            Long armazem,
            FatoServico f,
            LocalDate data,
            List<CalculoCobrancaDto.Pendencia> p) {
        var t = tabela(cliente, armazem, data, p);
        var i = itemServico(t, f.getServico().getId(), f.getCategoria());
        if (i == null)
            pendente(
                    p,
                    "TARIFA_SERVICO_AUSENTE",
                    data,
                    null,
                    f.getId(),
                    "Tarifa/categoria vigente no instante executado não configurada.");
        BigDecimal bruto =
                i == null
                        ? null
                        : i.getPercentual() == null
                                ? f.getQuantidade().multiply(i.getPreco())
                                : f.getValorBase() == null
                                        ? null
                                        : f.getValorBase()
                                                .multiply(i.getPercentual())
                                                .divide(CEM)
                                                .multiply(f.getQuantidade());
        BigDecimal valor = bruto == null ? null : moeda(bruto);
        var parcelas = new ArrayList<CalculoCobrancaDto.Parcela>();
        var cotas = rateios.findByFatoIdOrderByNotaIdAsc(f.getId());
        BigDecimal soma = cotas.stream().map(c -> c.getCota()).reduce(ZERO, BigDecimal::add);
        if (soma.signum() == 0) {
            pendente(
                    p, "RATEIO_INSUFICIENTE", data, null, f.getId(), "Fato sem parcelas por nota.");
            valor = null;
        }
        BigDecimal restante = valor;
        for (int n = 0; n < cotas.size(); n++) {
            var c = cotas.get(n);
            BigDecimal v =
                    valor == null
                            ? null
                            : n == cotas.size() - 1
                                    ? restante
                                    : valor.multiply(c.getCota())
                                            .divide(soma, 2, RoundingMode.DOWN);
            if (v != null) restante = restante.subtract(v);
            parcelas.add(new CalculoCobrancaDto.Parcela(c.getNota().getId(), c.getCota(), v));
        }
        return new CalculoCobrancaDto.Servico(
                f.getId(),
                f.getServico().getId(),
                f.getChaveFato(),
                f.getExecutadoEm(),
                t == null ? null : t.getId(),
                i == null ? null : i.getId(),
                f.getQuantidade(),
                i == null ? null : i.getPreco(),
                i == null ? null : i.getPercentual(),
                f.getValorBase(),
                valor,
                bruto,
                List.copyOf(parcelas));
    }

    private CalculoCobrancaDto.Ajustes ajustes(
            ContratoCobranca c,
            CalculoCobrancaDto.Calcular d,
            List<CalculoCobrancaDto.Diaria> ds,
            Map<Long, BigDecimal> subtotais,
            List<CalculoCobrancaDto.Pendencia> p) {
        if (c == null) return null;
        var ciclo = PeriodoCobrancaService.ciclo(c, d.periodoInicio());
        long dias = ChronoUnit.DAYS.between(d.periodoInicio(), d.periodoFim());
        if (d.periodoFim().isAfter(ciclo.fim()))
            pendente(
                    p,
                    "MULTIPLOS_CICLOS",
                    null,
                    null,
                    null,
                    "Calcule um ciclo nominal por vez; não repetir mínimos entre cortes.");
        var abrangencia =
                minimos.findByContratoId(c.getId()).stream()
                        .map(s -> s.getServico().getId())
                        .sorted()
                        .toList();
        BigDecimal elegivel =
                abrangencia.stream()
                        .map(id -> subtotais.getOrDefault(id, ZERO))
                        .reduce(ZERO, BigDecimal::add);
        BigDecimal minimo = null, minimoAplicavel = null;
        if (c.getMinimoModo().equals("NAO_INFORMADO"))
            pendente(
                    p,
                    "MINIMO_NAO_CONFIGURADO",
                    null,
                    null,
                    null,
                    "Informe aplicação ou não aplicação explicitamente.");
        else if (c.getMinimoModo().equals("NAO_APLICAVEL")) minimo = ZERO;
        else {
            minimoAplicavel = c.getMinimoValor();
            if (c.getMinimoProporcao().equals("PROPORCIONAL_DIAS"))
                minimoAplicavel =
                        moeda(
                                minimoAplicavel
                                        .multiply(BigDecimal.valueOf(dias))
                                        .divide(
                                                BigDecimal.valueOf(ciclo.dias()),
                                                12,
                                                RoundingMode.HALF_UP));
            minimo = moeda(minimoAplicavel.subtract(elegivel).max(ZERO));
        }
        BigDecimal gris = null, base = null;
        String regra = null;
        if (c.getGrisModo().equals("NAO_INFORMADO"))
            pendente(
                    p,
                    "GRIS_NAO_CONFIGURADO",
                    null,
                    null,
                    null,
                    "Informe aplicação ou não aplicação explicitamente.");
        else if (c.getGrisModo().equals("NAO_APLICAVEL")) gris = ZERO;
        else if (ds.stream().anyMatch(x -> x.valorEstoque() == null))
            pendente(
                    p,
                    "BASE_GRIS_INSUFICIENTE",
                    null,
                    null,
                    null,
                    "Valor histórico/destino de avaria necessário em cada dia.");
        else if (c.getGrisPeriodicidade().equals("DIARIA")) {
            base =
                    ds.stream()
                            .map(CalculoCobrancaDto.Diaria::valorEstoque)
                            .reduce(ZERO, BigDecimal::add);
            gris =
                    ds.stream()
                            .map(
                                    x ->
                                            moeda(
                                                    x.valorEstoque()
                                                            .multiply(c.getGrisPercentual())
                                                            .divide(CEM)))
                            .reduce(ZERO, BigDecimal::add);
            regra =
                    "Soma de percentual sobre cada pico diário incluído; sem segundo fator dias/ciclo.";
        } else {
            base =
                    c.getGrisBase().equals("VALOR_ESTOQUE_PICO")
                            ? ds.stream()
                                    .map(CalculoCobrancaDto.Diaria::valorEstoque)
                                    .max(BigDecimal::compareTo)
                                    .orElse(ZERO)
                            : ds.stream()
                                    .map(CalculoCobrancaDto.Diaria::valorEstoque)
                                    .reduce(ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(dias), 12, RoundingMode.HALF_UP);
            BigDecimal bruto = base.multiply(c.getGrisPercentual()).divide(CEM);
            if (c.getGrisProporcao().equals("PROPORCIONAL_DIAS"))
                bruto =
                        bruto.multiply(BigDecimal.valueOf(dias))
                                .divide(BigDecimal.valueOf(ciclo.dias()), 12, RoundingMode.HALF_UP);
            gris = moeda(bruto);
            regra =
                    "Percentual uma vez por ciclo sobre base explícita; proporção "
                            + c.getGrisProporcao()
                            + " ("
                            + dias
                            + "/"
                            + ciclo.dias()
                            + ").";
        }
        return new CalculoCobrancaDto.Ajustes(
                ciclo.inicio(),
                ciclo.fim(),
                dias,
                ciclo.dias(),
                abrangencia,
                moeda(elegivel),
                minimoAplicavel,
                minimo,
                c.getGrisBase(),
                c.getGrisPeriodicidade(),
                c.getGrisProporcao(),
                c.getGrisPercentual(),
                base,
                gris,
                regra);
    }

    public CalculoCobrancaDto.Resultado consultar(@NotNull @Positive Long id) {
        var escopo = calculos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        contextos.autorizar(escopo.getClienteId(), escopo.getArmazemId());
        return resposta(calculos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado));
    }

    public PaginaResponse<CalculoCobrancaDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        contextos.autorizar(clienteId, armazemId);
        return PaginaResponse.de(
                calculos.findByClienteIdAndArmazemId(
                        clienteId, armazemId, CadastroSupport.pagina(pagina, tamanho)),
                this::resposta);
    }

    private CalculoCobrancaDto.Resultado resposta(CalculoCobranca c) {
        return new CalculoCobrancaDto.Resultado(
                c.getId(),
                c.getCliente().getId(),
                c.getArmazem().getId(),
                c.getContrato() == null ? null : c.getContrato().getId(),
                c.getPeriodoInicio(),
                c.getPeriodoFim(),
                c.getFuso(),
                c.getMoeda(),
                c.getSituacao(),
                c.getRegraVersao(),
                c.getEntradasHash(),
                moeda(c.getSubtotalConhecido()),
                moeda(c.getMinimoCalculado()),
                moeda(c.getGrisCalculado()),
                moeda(c.getTotal()),
                c.getCalculadoEm(),
                mapper.readValue(
                        c.getPendenciasJson(),
                        new TypeReference<List<CalculoCobrancaDto.Pendencia>>() {}),
                mapper.readValue(c.getMemoriaJson(), CalculoCobrancaDto.Memoria.class));
    }

    private static BigDecimal moeda(BigDecimal v) {
        return v == null ? null : v.setScale(2, RoundingMode.HALF_UP);
    }

    private static void ponto(TreeSet<Instant> pontos, Instant valor, Instant inicio, Instant fim) {
        if (valor != null && !valor.isBefore(inicio) && valor.isBefore(fim)) pontos.add(valor);
    }

    private static void pendente(
            List<CalculoCobrancaDto.Pendencia> p,
            String codigo,
            LocalDate dia,
            Long unidade,
            Long fato,
            String detalhe) {
        var e = new CalculoCobrancaDto.Pendencia(codigo, dia, unidade, fato, detalhe);
        if (!p.contains(e)) p.add(e);
    }
}
