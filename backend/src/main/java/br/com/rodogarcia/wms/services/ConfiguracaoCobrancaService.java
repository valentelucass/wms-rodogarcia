package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ConfiguracaoCobrancaDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.ServicoMinimoContrato;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.CalculoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ItemTabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.MemoriaDiariaRepository;
import br.com.rodogarcia.wms.repositories.ServicoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ServicoMinimoContratoRepository;
import br.com.rodogarcia.wms.repositories.TabelaCobrancaRepository;
import br.com.rodogarcia.wms.repositories.VinculoTabelaClienteRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ConfiguracaoCobrancaService {
    private final ServicoCobrancaRepository servicos;
    private final TabelaCobrancaRepository tabelas;
    private final ItemTabelaCobrancaRepository itens;
    private final VinculoTabelaClienteRepository vinculos;
    private final ContratoCobrancaRepository contratos;
    private final ServicoMinimoContratoRepository minimos;
    private final ArmazemRepository armazens;
    private final CalculoCobrancaRepository calculos;
    private final MemoriaDiariaRepository diarias;
    private final ContextoCobrancaService contextos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final Clock clock;
    private final ResolucaoFinanceiraCadastroService resolucaoCadastro;

    public ConfiguracaoCobrancaService(
            ServicoCobrancaRepository servicos,
            TabelaCobrancaRepository tabelas,
            ItemTabelaCobrancaRepository itens,
            VinculoTabelaClienteRepository vinculos,
            ContratoCobrancaRepository contratos,
            ServicoMinimoContratoRepository minimos,
            ArmazemRepository armazens,
            CalculoCobrancaRepository calculos,
            MemoriaDiariaRepository diarias,
            ContextoCobrancaService contextos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            Clock clock,
            ResolucaoFinanceiraCadastroService resolucaoCadastro) {
        this.resolucaoCadastro = resolucaoCadastro;
        this.servicos = servicos;
        this.tabelas = tabelas;
        this.itens = itens;
        this.vinculos = vinculos;
        this.contratos = contratos;
        this.minimos = minimos;
        this.armazens = armazens;
        this.calculos = calculos;
        this.diarias = diarias;
        this.contextos = contextos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.clock = clock;
    }

    public PaginaResponse<ConfiguracaoCobrancaDto.Servico> servicos(int pagina, int tamanho) {
        acesso.exigirSupervisor();
        return PaginaResponse.de(
                servicos.findAll(CadastroSupport.pagina(pagina, tamanho)),
                ConfiguracaoCobrancaDto.Servico::de);
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Servico criarServico(
            @NotNull @Valid ConfiguracaoCobrancaDto.CriarServico d) {
        acesso.exigirGestor();
        String hash = operacoes.hash("CRIACAO_SERVICO", null, d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Servico.class);
        if (replay != null) return replay;
        if ("ARMAZENAGEM".equals(d.tipo()) != "POSICAO_DIA".equals(d.unidade()))
            throw CadastroSupport.invalido("POSICAO_DIA pertence exclusivamente à armazenagem.");
        if (!"ADICIONAL".equals(d.tipo())
                && List.of("VEICULO", "CONTEINER", "PERCENTUAL").contains(d.unidade()))
            throw CadastroSupport.invalido(
                    "Unidade adicional incompatível com este tipo de serviço.");
        String codigo = CadastroSupport.codigo(d.codigo());
        if (servicos.existsByCodigo(codigo)) throw conflito("SERVICO_DUPLICADO");
        Instant agora = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var s =
                servicos.saveAndFlush(
                        new ServicoCobranca(
                                codigo,
                                CadastroSupport.texto(d.descricao()),
                                d.tipo(),
                                d.unidade(),
                                "ATIVO",
                                agora,
                                agora));
        var r = ConfiguracaoCobrancaDto.Servico.de(s);
        auditoria.registrar(
                "SERVICO_COBRANCA",
                s.getId(),
                "CRIACAO",
                CadastroSupport.motivo(d.motivo()),
                null,
                r);
        operacoes.salvar(d.operacaoId(), "CRIACAO_SERVICO", null, null, s.getId(), hash, r);
        return r;
    }

    public ConfiguracaoCobrancaDto.Tabela tabela(@NotNull @Positive Long id) {
        acesso.exigirSupervisor();
        var t = tabelas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.armazem(t.getArmazem().getId());
        if (t.getCliente() != null) acesso.cliente(t.getCliente().getId());
        return resposta(t);
    }

    public List<ConfiguracaoCobrancaDto.Tabela> tabelas(@NotNull @Positive Long armazemId) {
        acesso.exigirSupervisor();
        acesso.armazem(armazemId);
        return tabelas.findByArmazemIdOrderByIdAsc(armazemId).stream()
                .filter(
                        t ->
                                t.getCliente() == null
                                        || acesso.gestor()
                                        || acesso.clientes().contains(t.getCliente().getId()))
                .map(this::resposta)
                .toList();
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Tabela criarTabela(
            @NotNull @Valid ConfiguracaoCobrancaDto.CriarTabela d) {
        acesso.exigirGestor();
        if ("ESPECIFICA".equals(d.tipo()) != (d.clienteId() != null))
            throw CadastroSupport.invalido(
                    "Tabela específica exige proprietário; padrão não possui proprietário.");
        acesso.armazem(d.armazemId());
        var c =
                d.clienteId() == null
                        ? null
                        : contextos.bloquear(d.clienteId(), d.armazemId()).cliente();
        var a =
                armazens.buscarParaAtualizar(d.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (c != null)
            resolucaoCadastro.conferir(new ContextoCobrancaService.Contexto(c, a), d.resolucao());
        else {
            if (d.resolucao() != null)
                throw CadastroSupport.invalido(
                        "Resolucao identificada de tabela requer proprietario especifico.");
            CadastroSupport.ativo(a);
        }
        conferirServicosHistoricos(
                d.itens().stream().map(ConfiguracaoCobrancaDto.Item::servicoId).toList(),
                d.resolucao(),
                d.vigenciaFim());
        String hash = operacoes.hash("CRIACAO_TABELA", null, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Tabela.class);
        if (replay != null) return replay;

        periodo(d.vigenciaInicio(), d.vigenciaFim());
        String codigo = CadastroSupport.codigo(d.codigo());
        if (tabelas.existsByArmazemIdAndCodigo(a.getId(), codigo))
            throw conflito("TABELA_DUPLICADA");
        var chaves = new HashSet<String>();
        var categorias = new HashMap<ConfiguracaoCobrancaDto.Item, String>();
        for (var i :
                d.itens().stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        ConfiguracaoCobrancaDto.Item::servicoId))
                        .toList()) {
            var s =
                    servicos.buscarParaAtualizar(i.servicoId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!"ATIVO".equals(s.getSituacao())
                    && (d.resolucao() == null
                            || d.vigenciaFim() == null
                            || d.vigenciaFim()
                                    .isAfter(
                                            s.getAlteradoEm()
                                                    .atZone(ZoneId.of("UTC"))
                                                    .toLocalDate())))
                throw conflito("SERVICO_INATIVO");
            String categoria = CadastroSupport.categoriaCobranca(i.categoria());
            categorias.put(i, categoria);
            if (!chaves.add(s.getId() + ":" + categoria)) throw conflito("ITEM_DUPLICADO");
            if ("PERCENTUAL".equals(s.getUnidade())
                    ? (i.percentual() == null || i.preco() != null)
                    : (i.preco() == null || i.percentual() != null))
                throw CadastroSupport.invalido(
                        "Configure preço ou percentual conforme unidade do serviço.");
            if ("ARMAZENAGEM".equals(s.getTipo())
                    && !List.of("", "PALLET", "BOBINA").contains(categoria))
                throw CadastroSupport.invalido(
                        "Categoria de armazenagem deve ser vazia, PALLET ou BOBINA.");
        }
        Instant agora = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var t =
                tabelas.saveAndFlush(
                        new TabelaCobranca(
                                a,
                                c,
                                codigo,
                                CadastroSupport.texto(d.descricao()),
                                d.tipo(),
                                d.vigenciaInicio(),
                                d.vigenciaFim(),
                                "ATIVA",
                                agora,
                                agora));
        for (var i : d.itens())
            itens.save(
                    new ItemTabelaCobranca(
                            t,
                            servicos.getReferenceById(i.servicoId()),
                            categorias.get(i),
                            i.preco(),
                            i.percentual()));
        itens.flush();
        var r = resposta(t);
        auditoria.registrar(
                "TABELA_COBRANCA",
                t.getId(),
                "CRIACAO",
                CadastroSupport.motivo(d.motivo()),
                null,
                resolucaoCadastro.paraAuditoria(d.resolucao(), r));
        operacoes.salvar(d.operacaoId(), "CRIACAO_TABELA", c, a, t.getId(), hash, r);
        return r;
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Tabela encerrarTabela(
            @NotNull @Positive Long id, @NotNull @Valid ConfiguracaoCobrancaDto.Encerrar d) {
        acesso.exigirGestor();
        Long aid = tabelas.buscarArmazemId(id).orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.armazem(aid);
        var original = tabelas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        ContextoCobrancaService.Contexto contexto = null;
        if (original.getCliente() != null) {
            contexto = contextos.bloquear(original.getCliente().getId(), aid);
            resolucaoCadastro.conferir(contexto, d.resolucao());
        } else if (d.resolucao() != null)
            throw CadastroSupport.invalido(
                    "Tabela compartilhada nao recebe resolucao de um proprietario.");
        var a = armazens.buscarParaAtualizar(aid).orElseThrow(RegraNegocioException::naoEncontrado);
        if (contexto == null) CadastroSupport.ativo(a);
        String hash = operacoes.hash("ENCERRAMENTO_VIGENCIA", List.of("TABELA", id), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Tabela.class);
        if (replay != null) return replay;
        var t = tabelas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        versao(t.getVersao(), d.versao());
        periodo(t.getVigenciaInicio(), d.vigenciaFim());
        if (t.getVigenciaFim() != null && d.vigenciaFim().isAfter(t.getVigenciaFim()))
            throw conflito("VIGENCIA_NAO_EXTENSIVEL");
        if (d.resolucao() == null
                && (diarias.existsByTabelaIdAndDataGreaterThanEqual(id, d.vigenciaFim())
                        || calculos.tabelaUsadaApos(id, d.vigenciaFim())))
            throw conflito("HISTORICO_CALCULADO");
        var antes = resposta(t);
        t.encerrar(
                d.vigenciaFim(),
                Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        tabelas.flush();
        var r = resposta(t);
        auditoria.registrar(
                "TABELA_COBRANCA",
                id,
                "ENCERRAMENTO_VIGENCIA",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resolucaoCadastro.paraAuditoria(d.resolucao(), r));
        operacoes.salvar(d.operacaoId(), "ENCERRAMENTO_VIGENCIA", t.getCliente(), a, id, hash, r);
        return r;
    }

    public List<ConfiguracaoCobrancaDto.Vinculo> vinculos(
            @NotNull @Positive Long clienteId, @NotNull @Positive Long armazemId) {
        contextos.autorizar(clienteId, armazemId);
        return vinculos
                .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(clienteId, armazemId)
                .stream()
                .map(ConfiguracaoCobrancaDto.Vinculo::de)
                .toList();
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Vinculo vincular(
            @NotNull @Valid ConfiguracaoCobrancaDto.Vincular d) {
        acesso.exigirGestor();
        var ctx = contextos.bloquear(d.clienteId(), d.armazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        String hash = operacoes.hash("VINCULO_TABELA", null, d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Vinculo.class);
        if (replay != null) return replay;

        periodo(d.vigenciaInicio(), d.vigenciaFim());
        var t = tabelas.findById(d.tabelaId()).orElseThrow(RegraNegocioException::naoEncontrado);
        if (!t.getArmazem().getId().equals(d.armazemId())
                || t.getCliente() != null && !t.getCliente().getId().equals(d.clienteId()))
            throw conflito("CONTEXTO_TABELA_INVALIDO");
        if (d.vigenciaInicio().isBefore(t.getVigenciaInicio())
                || t.getVigenciaFim() != null
                        && (d.vigenciaFim() == null || d.vigenciaFim().isAfter(t.getVigenciaFim())))
            throw conflito("VIGENCIA_FORA_TABELA");
        if (vinculos
                .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(d.clienteId(), d.armazemId())
                .stream()
                .anyMatch(
                        v ->
                                PeriodoCobrancaService.sobrepoe(
                                        v.getVigenciaInicio(),
                                        v.getVigenciaFim(),
                                        d.vigenciaInicio(),
                                        d.vigenciaFim()))) throw conflito("VIGENCIA_SOBREPOSTA");
        Instant agora = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var v =
                vinculos.saveAndFlush(
                        new VinculoTabelaCliente(
                                ctx.cliente(),
                                ctx.armazem(),
                                t,
                                d.vigenciaInicio(),
                                d.vigenciaFim(),
                                agora,
                                agora));
        var r = ConfiguracaoCobrancaDto.Vinculo.de(v);
        auditoria.registrar(
                "VINCULO_COBRANCA",
                v.getId(),
                "VINCULO_TABELA",
                CadastroSupport.motivo(d.motivo()),
                null,
                resolucaoCadastro.paraAuditoria(d.resolucao(), r));
        operacoes.salvar(
                d.operacaoId(), "VINCULO_TABELA", ctx.cliente(), ctx.armazem(), v.getId(), hash, r);
        return r;
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Vinculo encerrarVinculo(
            @NotNull @Positive Long id, @NotNull @Valid ConfiguracaoCobrancaDto.Encerrar d) {
        acesso.exigirGestor();
        var escopo = vinculos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var ctx = contextos.bloquear(escopo.getClienteId(), escopo.getArmazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        String hash = operacoes.hash("VINCULO_TABELA", id, d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Vinculo.class);
        if (replay != null) return replay;
        var v = vinculos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        versao(v.getVersao(), d.versao());
        periodo(v.getVigenciaInicio(), d.vigenciaFim());
        if (v.getVigenciaFim() != null && d.vigenciaFim().isAfter(v.getVigenciaFim()))
            throw conflito("VIGENCIA_NAO_EXTENSIVEL");
        if (d.resolucao() == null
                && calculos.contextoCalculadoApos(
                        ctx.cliente().getId(), ctx.armazem().getId(), d.vigenciaFim()))
            throw conflito("HISTORICO_CALCULADO");
        var antes = ConfiguracaoCobrancaDto.Vinculo.de(v);
        v.encerrar(
                d.vigenciaFim(),
                Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        vinculos.flush();
        var r = ConfiguracaoCobrancaDto.Vinculo.de(v);
        auditoria.registrar(
                "VINCULO_COBRANCA",
                id,
                "VINCULO_TABELA",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resolucaoCadastro.paraAuditoria(d.resolucao(), r));
        operacoes.salvar(
                d.operacaoId(), "VINCULO_TABELA", ctx.cliente(), ctx.armazem(), id, hash, r);
        return r;
    }

    public List<ConfiguracaoCobrancaDto.Contrato> contratos(
            @NotNull @Positive Long clienteId, @NotNull @Positive Long armazemId) {
        contextos.autorizar(clienteId, armazemId);
        return contratos
                .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(clienteId, armazemId)
                .stream()
                .map(this::resposta)
                .toList();
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Contrato configurar(
            @NotNull @Valid ConfiguracaoCobrancaDto.ConfigurarContrato d) {
        acesso.exigirGestor();
        var ctx = contextos.bloquear(d.clienteId(), d.armazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        conferirServicosHistoricos(d.servicosMinimo(), d.resolucao(), d.vigenciaFim());
        String hash = operacoes.hash("CONFIGURACAO", null, d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Contrato.class);
        if (replay != null) return replay;

        periodo(d.vigenciaInicio(), d.vigenciaFim());
        try {
            ZoneId.of(d.fuso());
        } catch (DateTimeException e) {
            throw CadastroSupport.invalido("Fuso civil inválido.");
        }
        if ("MES_DIA_FIXO".equals(d.modalidadeCiclo())
                ? (d.diaCorte() == null || d.duracaoDias() != null)
                : (d.duracaoDias() == null || d.diaCorte() != null))
            throw CadastroSupport.invalido("Configure corte ou duração conforme modalidade.");
        boolean minimo = "APLICAVEL".equals(d.minimoModo());
        if (minimo
                ? (d.minimoValor() == null
                        || d.minimoProporcao() == null
                        || d.servicosMinimo().isEmpty())
                : (d.minimoValor() != null
                        || d.minimoProporcao() != null
                        || !d.servicosMinimo().isEmpty()))
            throw CadastroSupport.invalido(
                    "Mínimo exige valor, proporção e abrangência explícitos somente quando aplicável.");
        boolean gris = "APLICAVEL".equals(d.grisModo());
        if (gris
                ? (d.grisPercentual() == null
                        || d.grisBase() == null
                        || d.grisPeriodicidade() == null
                        || d.grisProporcao() == null)
                : (d.grisPercentual() != null
                        || d.grisBase() != null
                        || d.grisPeriodicidade() != null
                        || d.grisProporcao() != null))
            throw CadastroSupport.invalido(
                    "GRIS exige percentual/base/periodicidade/proporção explícitos somente quando aplicável.");
        if (new HashSet<>(d.servicosMinimo()).size() != d.servicosMinimo().size())
            throw CadastroSupport.invalido("Serviço do mínimo repetido.");
        for (Long sid : d.servicosMinimo().stream().sorted().toList()) {
            var s =
                    servicos.buscarParaAtualizar(sid)
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!"ATIVO".equals(s.getSituacao())
                    && (d.resolucao() == null
                            || d.vigenciaFim() == null
                            || d.vigenciaFim()
                                    .isAfter(
                                            s.getAlteradoEm()
                                                    .atZone(ZoneId.of(d.fuso()))
                                                    .toLocalDate())))
                throw conflito("SERVICO_INATIVO");
        }
        var anteriores =
                contratos.findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
                        d.clienteId(), d.armazemId());
        if (anteriores.stream()
                .anyMatch(
                        c ->
                                PeriodoCobrancaService.sobrepoe(
                                        c.getVigenciaInicio(),
                                        c.getVigenciaFim(),
                                        d.vigenciaInicio(),
                                        d.vigenciaFim()))) throw conflito("VIGENCIA_SOBREPOSTA");
        var anterior =
                anteriores.stream()
                        .filter(c -> c.getVigenciaInicio().isBefore(d.vigenciaInicio()))
                        .reduce((a, b) -> b)
                        .orElse(null);
        if (anterior != null
                && !PeriodoCobrancaService.ciclo(anterior, d.vigenciaInicio())
                        .inicio()
                        .equals(d.vigenciaInicio())) throw conflito("ALTERACAO_FORA_CORTE");
        if (d.resolucao() == null
                && calculos.contextoCalculadoApos(d.clienteId(), d.armazemId(), d.vigenciaInicio()))
            throw conflito("HISTORICO_CALCULADO");
        Instant agora = Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        var c =
                contratos.saveAndFlush(
                        new ContratoCobranca(
                                ctx.cliente(),
                                ctx.armazem(),
                                d.vigenciaInicio(),
                                d.vigenciaFim(),
                                d.fuso(),
                                d.moeda(),
                                d.modalidadeCiclo(),
                                d.diaCorte(),
                                d.duracaoDias(),
                                d.minimoModo(),
                                d.grisModo(),
                                d.minimoValor(),
                                d.minimoProporcao(),
                                d.grisPercentual(),
                                d.grisBase(),
                                d.grisPeriodicidade(),
                                d.grisProporcao(),
                                agora,
                                agora));
        for (Long sid : d.servicosMinimo())
            minimos.save(new ServicoMinimoContrato(c, servicos.getReferenceById(sid)));
        minimos.flush();
        var r = resposta(c);
        auditoria.registrar(
                "CONTRATO_COBRANCA",
                c.getId(),
                "CONFIGURACAO",
                CadastroSupport.motivo(d.motivo()),
                null,
                resolucaoCadastro.paraAuditoria(d.resolucao(), r));
        operacoes.salvar(
                d.operacaoId(), "CONFIGURACAO", ctx.cliente(), ctx.armazem(), c.getId(), hash, r);
        return r;
    }

    @Transactional
    public ConfiguracaoCobrancaDto.Contrato encerrarContrato(
            @NotNull @Positive Long id, @NotNull @Valid ConfiguracaoCobrancaDto.Encerrar d) {
        acesso.exigirGestor();
        var escopo = contratos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var ctx = contextos.bloquear(escopo.getClienteId(), escopo.getArmazemId());
        resolucaoCadastro.conferir(ctx, d.resolucao());
        String hash = operacoes.hash("CONFIGURACAO", id, d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, ConfiguracaoCobrancaDto.Contrato.class);
        if (replay != null) return replay;
        var c = contratos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        versao(c.getVersao(), d.versao());
        periodo(c.getVigenciaInicio(), d.vigenciaFim());
        if (!PeriodoCobrancaService.ciclo(c, d.vigenciaFim()).inicio().equals(d.vigenciaFim()))
            throw conflito("ALTERACAO_FORA_CORTE");
        if (c.getVigenciaFim() != null && d.vigenciaFim().isAfter(c.getVigenciaFim()))
            throw conflito("VIGENCIA_NAO_EXTENSIVEL");
        if (d.resolucao() == null
                && calculos.contextoCalculadoApos(
                        ctx.cliente().getId(), ctx.armazem().getId(), d.vigenciaFim()))
            throw conflito("HISTORICO_CALCULADO");
        var antes = resposta(c);
        c.encerrar(
                d.vigenciaFim(),
                Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        contratos.flush();
        var r = resposta(c);
        auditoria.registrar(
                "CONTRATO_COBRANCA",
                id,
                "CONFIGURACAO",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resolucaoCadastro.paraAuditoria(d.resolucao(), r));
        operacoes.salvar(d.operacaoId(), "CONFIGURACAO", ctx.cliente(), ctx.armazem(), id, hash, r);
        return r;
    }

    private ConfiguracaoCobrancaDto.Tabela resposta(TabelaCobranca t) {
        return ConfiguracaoCobrancaDto.Tabela.de(t, itens.findByTabelaIdOrderByIdAsc(t.getId()));
    }

    private ConfiguracaoCobrancaDto.Contrato resposta(ContratoCobranca c) {
        return ConfiguracaoCobrancaDto.Contrato.de(
                c,
                minimos.findByContratoId(c.getId()).stream()
                        .map(s -> s.getServico().getId())
                        .sorted()
                        .toList());
    }

    private static void periodo(LocalDate inicio, LocalDate fim) {
        if (fim != null && !fim.isAfter(inicio))
            throw CadastroSupport.invalido("Vigência usa intervalo [início,fim) não vazio.");
    }

    private void conferirServicosHistoricos(
            List<Long> ids,
            br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Resolucao resolucao,
            LocalDate fim) {
        for (Long id : ids.stream().distinct().sorted().toList()) {
            var s =
                    servicos.buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!"ATIVO".equals(s.getSituacao())
                    && (resolucao == null
                            || fim == null
                            || fim.isAfter(
                                    s.getAlteradoEm().atZone(ZoneId.of("UTC")).toLocalDate())))
                throw conflito("RESOLUCAO_FINANCEIRA_OBRIGATORIA");
        }
    }

    private static void versao(long atual, long informada) {
        if (atual != informada) throw conflito("VERSAO_DESATUALIZADA");
    }

    private static RegraNegocioException conflito(String codigo) {
        return RegraNegocioException.conflito(
                codigo, "Configuração conflitante; consulte vigências, contexto e histórico.");
    }
}
