package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Ajustar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Ajuste;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Ciclo;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Confirmacao;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.ConfirmacaoComando;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Confirmar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Decidir;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Demonstrativo;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Documento;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.DocumentoConferido;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Entrega;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Entregar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Fechamento;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Identificacao;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Nfse;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Preparar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Reabrir;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Resolucao;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.ResolucaoSaldo;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Resolver;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Tratar;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Tratativa;
import br.com.rodogarcia.wms.dto.FechamentoCobrancaDto.Versao;
import br.com.rodogarcia.wms.dto.FiscalCadastroDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.AjusteFechamento;
import br.com.rodogarcia.wms.models.AjusteVersaoFechamento;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.CalculoCobranca;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ConfirmacaoExternaFechamento;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.DiaFechamento;
import br.com.rodogarcia.wms.models.EntregaEsl;
import br.com.rodogarcia.wms.models.FatoFechamento;
import br.com.rodogarcia.wms.models.FechamentoCobranca;
import br.com.rodogarcia.wms.models.ReferenciaNfse;
import br.com.rodogarcia.wms.models.ResolucaoFinanceiraFechamento;
import br.com.rodogarcia.wms.models.TratativaExternaFechamento;
import br.com.rodogarcia.wms.models.VersaoFechamento;
import br.com.rodogarcia.wms.repositories.AjusteFechamentoRepository;
import br.com.rodogarcia.wms.repositories.AjusteVersaoFechamentoRepository;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.repositories.CalculoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ConfirmacaoExternaFechamentoRepository;
import br.com.rodogarcia.wms.repositories.ContratoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.DiaFechamentoRepository;
import br.com.rodogarcia.wms.repositories.EntregaEslRepository;
import br.com.rodogarcia.wms.repositories.FatoFechamentoRepository;
import br.com.rodogarcia.wms.repositories.FatoServicoRepository;
import br.com.rodogarcia.wms.repositories.FechamentoCobrancaRepository;
import br.com.rodogarcia.wms.repositories.ReferenciaNfseRepository;
import br.com.rodogarcia.wms.repositories.ResolucaoFinanceiraFechamentoRepository;
import br.com.rodogarcia.wms.repositories.TratativaExternaFechamentoRepository;
import br.com.rodogarcia.wms.repositories.VersaoFechamentoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

/** Fechamento local e referências externas manuais; nenhum envio ou emissão fiscal. */
@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FechamentoCobrancaService {
    private final FechamentoCobrancaRepository fechamentos;
    private final VersaoFechamentoRepository versoes;
    private final DiaFechamentoRepository dias;
    private final FatoFechamentoRepository fatos;
    private final FatoServicoRepository fatosServico;
    private final AjusteFechamentoRepository ajustes;
    private final AjusteVersaoFechamentoRepository composicoes;
    private final EntregaEslRepository entregas;
    private final ConfirmacaoExternaFechamentoRepository confirmacoes;
    private final ReferenciaNfseRepository documentos;
    private final ResolucaoFinanceiraFechamentoRepository resolucoes;
    private final CalculoCobrancaRepository calculos;
    private final ContratoCobrancaRepository contratos;
    private final CalculoCobrancaService apurador;
    private final ContextoCobrancaService contextos;
    private final ResolucaoFinanceiraCadastroService cadastros;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;
    private final TratativaExternaFechamentoRepository tratativas;
    private final jakarta.persistence.EntityManager entityManager;
    private final AuditoriaCadastroRepository historicoAuditoria;

    public FechamentoCobrancaService(
            FechamentoCobrancaRepository fechamentos,
            VersaoFechamentoRepository versoes,
            DiaFechamentoRepository dias,
            FatoFechamentoRepository fatos,
            FatoServicoRepository fatosServico,
            AjusteFechamentoRepository ajustes,
            AjusteVersaoFechamentoRepository composicoes,
            EntregaEslRepository entregas,
            ConfirmacaoExternaFechamentoRepository confirmacoes,
            ReferenciaNfseRepository documentos,
            ResolucaoFinanceiraFechamentoRepository resolucoes,
            CalculoCobrancaRepository calculos,
            ContratoCobrancaRepository contratos,
            CalculoCobrancaService apurador,
            ContextoCobrancaService contextos,
            ResolucaoFinanceiraCadastroService cadastros,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock,
            TratativaExternaFechamentoRepository tratativas,
            jakarta.persistence.EntityManager entityManager,
            AuditoriaCadastroRepository historicoAuditoria) {
        this.entityManager = entityManager;
        this.historicoAuditoria = historicoAuditoria;
        this.tratativas = tratativas;
        this.fechamentos = fechamentos;
        this.versoes = versoes;
        this.dias = dias;
        this.fatos = fatos;
        this.fatosServico = fatosServico;
        this.ajustes = ajustes;
        this.composicoes = composicoes;
        this.entregas = entregas;
        this.confirmacoes = confirmacoes;
        this.documentos = documentos;
        this.resolucoes = resolucoes;
        this.calculos = calculos;
        this.contratos = contratos;
        this.apurador = apurador;
        this.contextos = contextos;
        this.cadastros = cadastros;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
    }

    public PaginaResponse<Fechamento> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        contextos.autorizar(clienteId, armazemId);
        return PaginaResponse.de(
                fechamentos.findByClienteIdAndArmazemId(
                        clienteId, armazemId, CadastroSupport.pagina(pagina, tamanho)),
                this::resposta);
    }

    public Fechamento consultar(@NotNull @Positive Long id) {
        return resposta(ler(id));
    }

    public List<Versao> versoes(@NotNull @Positive Long id) {
        ler(id);
        return versoes.findByFechamentoIdOrderByNumeroAsc(id).stream().map(this::resposta).toList();
    }

    public Versao versao(@NotNull @Positive Long id, @Positive int numero) {
        return resposta(versao(ler(id), numero));
    }

    public byte[] demonstrativo(@NotNull @Positive Long id, @Positive int numero) {
        return versao(ler(id), numero).getMemoriaJson().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ConfirmacaoComando preparar(@NotNull @Valid Preparar d) {
        acesso.exigirGestor();
        var calc = calculo(d.calculoId());
        var ctx = contextos.bloquear(calc.getCliente().getId(), calc.getArmazem().getId());
        cadastros.conferir(ctx, d.resolucao());
        cadastros.conferirPeriodo(
                d.resolucao(),
                calc.getPeriodoInicio(),
                calc.getPeriodoFim(),
                clock,
                calc.getFuso());
        String hash = operacoes.hash("PREPARACAO_FECHAMENTO", null, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        var c = calc.getContrato();
        if (c == null)
            throw conflito(
                    "CONTRATO_AUSENTE", "Contrato comprovado é necessário para derivar o corte.");
        var anteriores =
                fechamentos.findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc(
                        ctx.cliente().getId(), ctx.armazem().getId());
        LocalDate inicio =
                anteriores.isEmpty()
                        ? primeiroContrato(ctx).getVigenciaInicio()
                        : anteriores.getLast().getPeriodoFim();
        var contrato = contrato(ctx, inicio);
        var ciclo = PeriodoCobrancaService.ciclo(contrato, inicio);
        LocalDate fim = ciclo.fim();
        if (contrato.getVigenciaFim() != null && fim.isAfter(contrato.getVigenciaFim()))
            fim = contrato.getVigenciaFim();
        if (!inicio.equals(calc.getPeriodoInicio())
                || !fim.equals(calc.getPeriodoFim())
                || !c.getId().equals(contrato.getId()))
            throw conflito(
                    "PERIODO_FORA_DO_CORTE",
                    "Início deve seguir o último fim e término deve seguir o contrato/corte nominal.");
        var f =
                fechamentos.saveAndFlush(
                        new FechamentoCobranca(
                                ctx.cliente(),
                                ctx.armazem(),
                                contrato,
                                inicio,
                                fim,
                                "EM_REVISAO",
                                1,
                                agora(),
                                agora()));
        for (LocalDate dia = inicio; dia.isBefore(fim); dia = dia.plusDays(1))
            dias.save(new DiaFechamento(f, ctx.cliente(), ctx.armazem(), dia));
        dias.flush();
        var v = novaVersao(f, calc, 1);
        return concluir(
                f,
                v,
                d.operacaoId(),
                "PREPARACAO_FECHAMENTO",
                d.motivo(),
                null,
                hash,
                d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando aprovar(@NotNull @Positive Long id, @NotNull @Valid Decidir d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("APROVACAO_FECHAMENTO", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersao(f, d.versao(), d.numero());
        conferirSemConflito(f);
        var v = versao(f, d.numero());
        var antes = resposta(v);
        if (!v.getSituacao().equals("PENDENTE_REVISAO"))
            throw conflito("TRANSICAO_INVALIDA", "Aprovação integral exige a versão em revisão.");
        if (v.getSaldo() == null || !v.getCalculo().getSituacao().equals("COMPLETO"))
            throw conflito(
                    "CALCULO_PENDENTE",
                    "Todas as parcelas/configurações precisam estar comprovadas.");
        if (f.getPeriodoFim()
                .isAfter(LocalDate.now(clock.withZone(ZoneId.of(v.getCalculo().getFuso())))))
            throw conflito("PERIODO_NAO_ENCERRADO", "O corte civil ainda não ocorreu.");
        apurador.conferirAtual(v.getCalculo());
        conferirComposicao(v);
        v.decidir("APROVADA", acesso.usuario(), CadastroSupport.motivo(d.motivo()), agora());
        f.atualizar("APROVADO", v.getNumero(), agora());
        return concluir(
                f,
                v,
                d.operacaoId(),
                "APROVACAO_FECHAMENTO",
                d.motivo(),
                antes,
                hash,
                d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando rejeitar(@NotNull @Positive Long id, @NotNull @Valid Decidir d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("REJEICAO_FECHAMENTO", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersao(f, d.versao(), d.numero());
        conferirSemConflito(f);
        var v = versao(f, d.numero());
        var antes = resposta(v);
        if (!v.getSituacao().equals("PENDENTE_REVISAO"))
            throw conflito("TRANSICAO_INVALIDA", "Rejeição integral exige revisão.");
        v.decidir("REJEITADA", acesso.usuario(), CadastroSupport.motivo(d.motivo()), agora());
        f.atualizar("REJEITADO", v.getNumero(), agora());
        return concluir(
                f,
                v,
                d.operacaoId(),
                "REJEICAO_FECHAMENTO",
                d.motivo(),
                antes,
                hash,
                d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando reabrir(@NotNull @Positive Long id, @NotNull @Valid Reabrir d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("REABERTURA_FECHAMENTO", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersao(f, d.versao(), d.numero());
        conferirSemConflito(f);
        var antiga = versao(f, d.numero());
        var antes = resposta(antiga);
        conferirReabertura(antiga);
        var calc = calculo(d.calculoId());
        conferirCalculo(f, calc);
        antiga.situacao("SUPERADA");
        var nova = novaVersao(f, calc, antiga.getNumero() + 1);
        f.atualizar("EM_REVISAO", nova.getNumero(), agora());
        return concluir(
                f,
                nova,
                d.operacaoId(),
                "REABERTURA_FECHAMENTO",
                d.motivo(),
                antes,
                hash,
                d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando entregar(@NotNull @Positive Long id, @NotNull @Valid Entregar d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("ENTREGA_ESL", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersao(f, d.versao(), d.numero());
        conferirSemConflito(f);
        var v = versao(f, d.numero());
        var antes = resposta(v);
        exigirAprovada(v);
        apurador.conferirAtual(v.getCalculo());
        conferirComposicao(v);
        if (d.layoutVersao() != 1 || !v.getConteudoHash().equalsIgnoreCase(d.arquivoHash()))
            throw conflito(
                    "ARQUIVO_DIVERGENTE", "Hash/layout devem corresponder ao arquivo aprovado.");
        conferirData(d.entregueEm(), v.getDecididaEm());
        var es = entregas.findByVersaoIdOrderBySequenciaAsc(v.getId());
        if (!es.isEmpty() && d.entregueEm().isBefore(es.getLast().getEntregueEm()))
            throw conflito(
                    "ENTREGA_FORA_DE_ORDEM", "Nova tentativa deve suceder a última entrega.");
        entregas.saveAndFlush(
                new EntregaEsl(
                        v,
                        es.size() + 1,
                        1,
                        v.getConteudoHash(),
                        texto(d.destinoReferencia()),
                        d.entregueEm(),
                        agora(),
                        acesso.usuario(),
                        CadastroSupport.motivo(d.motivo())));
        v.externo("DESCONHECIDO");
        return concluir(
                f, v, d.operacaoId(), "ENTREGA_ESL", d.motivo(), antes, hash, d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando confirmarNaoEmissao(
            @NotNull @Positive Long id, @NotNull @Valid Confirmar d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("CONFIRMACAO_EXTERNA", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersao(f, d.versao(), d.numero());
        conferirSemConflito(f);
        var v = versao(f, d.numero());
        var antes = resposta(v);
        exigirAprovada(v);
        conferirData(d.confirmadaEm(), v.getCriadaEm());
        EntregaEsl entrega = null;
        if (d.entregaId() != null) {
            entrega =
                    entregas.findById(d.entregaId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!entrega.getVersao().getId().equals(v.getId()))
                throw conflito("CONTEXTO_DIVERGENTE", "Entrega pertence a outra versão.");
        }
        var es = entregas.findByVersaoIdOrderBySequenciaAsc(v.getId());
        if (!es.isEmpty() && d.confirmadaEm().isBefore(es.getLast().getEntregueEm()))
            throw conflito(
                    "DECLARACAO_ANTERIOR_ENTREGA", "Declaração precisa cobrir a última entrega.");
        confirmacoes.saveAndFlush(
                new ConfirmacaoExternaFechamento(
                        v,
                        entrega,
                        "NAO_EMITIDO_CONFIRMADO",
                        texto(d.fonte()),
                        texto(d.confirmadaPor()),
                        d.confirmadaEm(),
                        agora(),
                        acesso.usuario(),
                        CadastroSupport.motivo(d.motivo())));
        v.externo("NAO_EMITIDO_CONFIRMADO");
        tentarFinalizarSemEmissao(f, v);
        return concluir(
                f,
                v,
                d.operacaoId(),
                "CONFIRMACAO_EXTERNA",
                d.motivo(),
                antes,
                hash,
                d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando registrarNfse(@NotNull @Positive Long id, @NotNull @Valid Nfse d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("REFERENCIA_NFSE", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersaoLock(f, d.versao());
        var v = versao(f, d.numero());
        var antes = resposta(v);
        boolean tardia =
                v.getNumero() != f.getVersaoAtual()
                        || v.getSituacao().equals("FINALIZADA_SEM_EMISSAO");
        boolean aprovadaHistoricamente = aprovadaHistoricamente(v);
        if (!aprovadaHistoricamente)
            throw conflito(
                    "VERSAO_NAO_APROVADA",
                    "Referência exige versão com aprovação/entrega histórica comprovada.");
        conferirData(d.emitidaEm(), v.getDecididaEm());
        String emissor = CadastroSupport.documento(d.emissorDocumento()),
                referencia = texto(d.referenciaExterna());
        if (documentos.existsByEmissorDocumentoAndReferenciaExterna(emissor, referencia))
            throw conflito("NFSE_DUPLICADA", "Identidade externa ou versão já vinculada.");
        boolean desatualizado = !apurador.atual(v.getCalculo());
        documentos.saveAndFlush(
                new ReferenciaNfse(
                        v,
                        emissor,
                        referencia,
                        nulo(d.numeroDocumento()),
                        nulo(d.serie()),
                        d.emitidaEm(),
                        agora(),
                        texto(d.fonte()),
                        texto(d.conferidaPor()),
                        acesso.usuario(),
                        CadastroSupport.motivo(d.motivo())));
        v.externo("EMITIDO");
        if (tardia
                || desatualizado
                || documentosConhecidos(f).size() > 1
                || f.getSituacao().equals("CONFLITO_EXTERNO")
                || v.getSaldo() == null
                || v.getSaldo().signum() <= 0) {
            f.atualizar("CONFLITO_EXTERNO", f.getVersaoAtual(), agora());
        } else {
            v.situacao("EMITIDA");
            f.atualizar("EMITIDO", v.getNumero(), agora());
            aplicar(v);
        }
        return concluir(
                f, v, d.operacaoId(), "REFERENCIA_NFSE", d.motivo(), antes, hash, d.resolucao());
    }

    @Transactional
    public ConfirmacaoComando resolverSaldo(
            @NotNull @Positive Long id, @NotNull @Valid Resolver d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("RESOLUCAO_FINANCEIRA", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersao(f, d.versao(), d.numero());
        conferirSemConflito(f);
        var v = versao(f, d.numero());
        var antes = resposta(v);
        exigirAprovada(v);
        if (v.getSaldo() == null || v.getSaldo().signum() > 0)
            throw conflito(
                    "SALDO_INCOMPATIVEL", "Resolução sem emissão é específica para ZERO/CREDITO.");
        conferirData(d.confirmadaEm(), v.getDecididaEm());
        if (resolucoes.findByVersaoId(v.getId()).isPresent())
            throw conflito("RESOLUCAO_DUPLICADA", "Versão já tem resolução financeira.");
        resolucoes.saveAndFlush(
                new ResolucaoFinanceiraFechamento(
                        v,
                        v.getSaldo().signum() == 0 ? "SALDO_ZERO" : "SALDO_CREDOR",
                        texto(d.referenciaExterna()),
                        texto(d.confirmadaPor()),
                        d.confirmadaEm(),
                        agora(),
                        texto(d.fonte()),
                        acesso.usuario(),
                        CadastroSupport.motivo(d.motivo())));
        tentarFinalizarSemEmissao(f, v);
        return concluir(
                f,
                v,
                d.operacaoId(),
                "RESOLUCAO_FINANCEIRA",
                d.motivo(),
                antes,
                hash,
                d.resolucao());
    }

    @Transactional
    public Ajuste ajustar(@NotNull @Valid Ajustar d) {
        acesso.exigirGestor();
        var origem =
                versoes.findById(d.origemVersaoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var escopo =
                fechamentos
                        .buscarEscopo(origem.getFechamento().getId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var ctx = contextos.bloquear(escopo.getClienteId(), escopo.getArmazemId());
        cadastros.conferir(ctx, d.resolucao());
        var de =
                fechamentos
                        .buscarEscopo(d.destinoFechamentoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        contextos.autorizar(de.getClienteId(), de.getArmazemId());
        if (!de.getClienteId().equals(escopo.getClienteId())
                || !de.getArmazemId().equals(escopo.getArmazemId()))
            throw conflito("CONTEXTO_DIVERGENTE", "Ajuste não transfere valores entre contextos.");
        for (Long id :
                java.util.stream.Stream.of(origem.getFechamento().getId(), d.destinoFechamentoId())
                        .distinct()
                        .sorted()
                        .toList())
            fechamentos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
        String hash = operacoes.hash("AJUSTE_FECHAMENTO", d.origemVersaoId(), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, Ajuste.class);
        if (replay != null) return replay;
        var original = origem.getFechamento();
        conferirSemConflito(original);
        if (original.getVersaoAtual() != origem.getNumero()
                || (!finalizada(origem)
                        && !(origem.getSituacao().equals("SUPERADA")
                                && origem.getEstadoExterno().equals("EMITIDO")
                                && original.getSituacao().equals("EMITIDO")
                                && original.getVersaoAtual() == origem.getNumero())))
            throw conflito(
                    "ORIGEM_NAO_FINALIZADA",
                    "Origem deve estar EMITIDA ou FINALIZADA_SEM_EMISSAO.");
        var destino =
                fechamentos
                        .findById(d.destinoFechamentoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        conferirVersaoLock(destino, d.versaoDestino());
        conferirSemConflito(destino);
        var elegiveis =
                fechamentos
                        .findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc(
                                escopo.getClienteId(), escopo.getArmazemId())
                        .stream()
                        .filter(
                                f ->
                                        !f.getPeriodoInicio().isBefore(original.getPeriodoFim())
                                                && !Set.of("EMITIDO", "FINALIZADO_SEM_EMISSAO")
                                                        .contains(f.getSituacao()))
                        .toList();
        if (elegiveis.isEmpty() || !elegiveis.getFirst().getId().equals(destino.getId()))
            throw conflito(
                    "DESTINO_FORA_DA_SEQUENCIA",
                    "Ajuste pertence ao primeiro ciclo posterior ainda não finalizado.");
        if (!Set.of("EM_REVISAO", "REJEITADO").contains(destino.getSituacao()))
            throw conflito(
                    "DESTINO_EXIGE_REABERTURA",
                    "Destino aprovado exige reabertura válida antes de mudar sua composição.");
        var corrigido = calculo(d.calculoCorrigidoId());
        conferirCalculo(original, corrigido);
        if (!corrigido.getSituacao().equals("COMPLETO"))
            throw conflito("CALCULO_PENDENTE", "Correção exige todas as parcelas comprovadas.");
        apurador.conferirAtual(corrigido);
        var historico =
                ajustes.findByOrigemVersaoFechamentoIdOrderByIdAsc(original.getId()).stream()
                        .filter(a -> a.getTipo().equals("CORRECAO_CALCULO"))
                        .toList();
        if (historico.stream()
                .anyMatch(a -> a.getHashCorrecao().equals(corrigido.getEntradasHash())))
            throw conflito("AJUSTE_DUPLICADO", "Memória corrigida já está vinculada à origem.");
        var base =
                historico.isEmpty()
                        ? origem.getCalculo()
                        : historico.getLast().getCalculoCorrigido();
        if (corrigido.getCalculadoEm().isBefore(base.getCalculadoEm())
                || corrigido.getCalculadoEm().equals(base.getCalculadoEm())
                        && corrigido.getId() <= base.getId())
            throw conflito(
                    "CORRECAO_FORA_DE_ORDEM", "Correcao deve suceder a ultima base validada.");
        var ajuste =
                ajustes.saveAndFlush(
                        new AjusteFechamento(
                                origem,
                                destino,
                                base,
                                corrigido,
                                null,
                                corrigido.getEntradasHash(),
                                base.getTotal(),
                                corrigido.getTotal(),
                                corrigido.getTotal().subtract(base.getTotal()),
                                "VALIDADO",
                                CadastroSupport.motivo(d.motivo()),
                                texto(d.evidencia()),
                                acesso.usuario(),
                                agora(),
                                null));
        vincularFatos(original, corrigido);
        var antiga = versao(destino, destino.getVersaoAtual());
        antiga.situacao("SUPERADA");
        var nova = novaVersao(destino, antiga.getCalculo(), antiga.getNumero() + 1);
        destino.atualizar("EM_REVISAO", nova.getNumero(), agora());
        versoes.flush();
        fechamentos.flush();
        entityManager.lock(destino, jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        var r = resposta(ajuste);
        auditoria.registrar(
                "FECHAMENTO_COBRANCA",
                destino.getId(),
                "AJUSTE_FECHAMENTO",
                CadastroSupport.motivo(d.motivo()),
                null,
                cadastros.paraAuditoria(
                        d.resolucao(),
                        new Object[] {r, resposta(nova)},
                        original.getPeriodoInicio(),
                        original.getPeriodoFim()));
        operacoes.salvar(
                d.operacaoId(),
                "AJUSTE_FECHAMENTO",
                ctx.cliente(),
                ctx.armazem(),
                ajuste.getId(),
                hash,
                r);
        return r;
    }

    @Transactional
    public ConfirmacaoComando tratarExterno(@NotNull @Positive Long id, @NotNull @Valid Tratar d) {
        var f = bloquear(id, d.resolucao());
        String hash = operacoes.hash("TRATATIVA_EXTERNA", id, d);
        var replay = operacoes.repetida(d.operacaoId(), hash, ConfirmacaoComando.class);
        if (replay != null) return replay;
        conferirVersaoLock(f, d.versao());
        if (!f.getSituacao().equals("CONFLITO_EXTERNO"))
            throw conflito("TRANSICAO_INVALIDA", "Tratativa exige conflito externo identificado.");
        var alvo = versao(f, d.numeroResultado());
        var antes = resposta(alvo);
        var conhecidos = documentosConhecidos(f);
        var porId = new HashMap<Long, DocumentoConferido>();
        for (var r : d.referencias())
            if (porId.put(r.referenciaId(), r) != null)
                throw CadastroSupport.invalido("Referência repetida na conferência.");
        if (!porId.keySet()
                .equals(new HashSet<>(conhecidos.stream().map(ReferenciaNfse::getId).toList())))
            throw conflito(
                    "CONFERENCIA_EXTERNA_INCOMPLETA",
                    "Informe exatamente todas as referências conhecidas deste fechamento.");
        Instant marco =
                conhecidos.stream()
                        .map(ReferenciaNfse::getEmitidaEm)
                        .max(Instant::compareTo)
                        .orElseThrow();
        conferirData(d.conferidaEm(), marco);
        var mantidos =
                conhecidos.stream()
                        .filter(r -> porId.get(r.getId()).situacao().equals("MANTIDO"))
                        .toList();
        if (d.resultado().equals("EMITIDO")) {
            if (mantidos.size() != 1
                    || !mantidos.getFirst().getVersao().getId().equals(alvo.getId()))
                throw conflito(
                        "REFERENCIAS_INCOMPATIVEIS",
                        "Resultado emitido exige uma referência mantida na versão indicada.");
        } else if (!mantidos.isEmpty() || alvo.getNumero() != f.getVersaoAtual())
            throw conflito(
                    "REFERENCIAS_INCOMPATIVEIS",
                    "Não emissão exige todos os cancelamentos comprovados e a versão vigente.");
        var baseAnterior = versao(f, f.getVersaoAtual());
        var originados = ajustes.findByOrigemVersaoFechamentoIdOrderByIdAsc(f.getId());
        var dependenciasOrigem = originados.stream().map(this::resposta).toList();
        boolean trocaComDependencias =
                !baseAnterior.getId().equals(alvo.getId()) && !originados.isEmpty();
        FechamentoCobranca destinoOrigem = null;
        BigDecimal deltaOrigem = null;
        if (trocaComDependencias) {
            var reg = d.regularizacaoOrigem();
            if (reg == null)
                throw conflito(
                        "AJUSTES_ORIGINADOS_PENDENTES",
                        "Troca da base exige regularizacao identificada de todos os deltas originados.");
            if (new HashSet<>(reg.ajustesDependentesIds()).size()
                            != reg.ajustesDependentesIds().size()
                    || !new HashSet<>(reg.ajustesDependentesIds())
                            .equals(
                                    new HashSet<>(
                                            originados.stream()
                                                    .map(AjusteFechamento::getId)
                                                    .toList())))
                throw conflito(
                        "DEPENDENCIAS_ORIGEM_DIVERGENTES",
                        "Confira exatamente todos os ajustes originados em todas as versoes.");
            if (baseAnterior.getCalculo().getTotal() == null
                    || alvo.getCalculo().getTotal() == null)
                throw conflito(
                        "CALCULO_PENDENTE",
                        "Regularizacao exige as duas bases proprias comprovadas.");
            deltaOrigem =
                    baseAnterior.getCalculo().getTotal().subtract(alvo.getCalculo().getTotal());
            if (deltaOrigem.compareTo(reg.diferencaEsperada()) != 0)
                throw conflito(
                        "DELTA_ORIGEM_DIVERGENTE",
                        "Diferenca conferida nao corresponde a troca das bases proprias.");
            destinoOrigem = destinoRegularizacao(f, reg.destinoFechamentoId(), reg.versaoDestino());
        } else if (d.regularizacaoOrigem() != null)
            throw CadastroSupport.invalido(
                    "Nao ha troca de base com deltas originados a regularizar.");
        for (var a : ajustes.findByDestinoFechamentoIdOrderByIdAsc(f.getId()))
            if (a.getSituacao().equals("APLICADO")
                    && !a.getAplicadoVersao().getId().equals(alvo.getId()))
                throw conflito(
                        "APLICACAO_JA_FINALIZADA",
                        "Não transferir nem repetir ajuste já aplicado em outra versão.");
        for (var x : composicoes.findByVersaoIdOrderByIdAsc(alvo.getId())) {
            var a = x.getAjuste();
            if (!a.getDestinoFechamento().getId().equals(f.getId()))
                throw conflito(
                        "COMPOSICAO_HISTORICA_REDIRECIONADA",
                        "Snapshot contém ajuste atualmente comprometido em outro destino; conflito permanece até reconciliação externa compatível.");
            if (a.getSituacao().equals("APLICADO")
                    && !a.getAplicadoVersao().getId().equals(alvo.getId()))
                throw conflito(
                        "APLICACAO_JA_FINALIZADA",
                        "Ajuste já aplicado em outra versão financeira.");
        }
        var faltantes =
                ajustes.findByDestinoFechamentoIdOrderByIdAsc(f.getId()).stream()
                        .filter(
                                a ->
                                        a.getSituacao().equals("VALIDADO")
                                                && composicoes
                                                        .findByVersaoIdOrderByIdAsc(alvo.getId())
                                                        .stream()
                                                        .noneMatch(
                                                                x ->
                                                                        x.getAjuste()
                                                                                .getId()
                                                                                .equals(a.getId())))
                        .toList();
        var atribuicaoAnterior = faltantes.stream().map(this::resposta).toList();
        ConfirmacaoComando destinoAlterado = null;
        if ((d.destinoAjustesId() == null) != (d.versaoDestinoAjustes() == null))
            throw CadastroSupport.invalido(
                    "Destino de regularização e versão são obrigatórios juntos.");
        if (!faltantes.isEmpty()) {
            if (d.destinoAjustesId() == null)
                throw conflito(
                        "AJUSTES_FORA_COMPOSICAO",
                        "Ajuste validado ausente exige destino posterior identificado; conflito permanece pendente.");
            var destinos =
                    fechamentos
                            .findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc(
                                    f.getCliente().getId(), f.getArmazem().getId())
                            .stream()
                            .filter(
                                    x ->
                                            !x.getPeriodoInicio().isBefore(f.getPeriodoFim())
                                                    && !Set.of("EMITIDO", "FINALIZADO_SEM_EMISSAO")
                                                            .contains(x.getSituacao()))
                            .toList();
            if (destinos.isEmpty() || !destinos.getFirst().getId().equals(d.destinoAjustesId()))
                throw conflito(
                        "DESTINO_FORA_DA_SEQUENCIA",
                        "Regularização exige o primeiro ciclo posterior não finalizado do mesmo contexto.");
            var destino =
                    fechamentos
                            .buscarParaAtualizar(d.destinoAjustesId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            conferirVersaoLock(destino, d.versaoDestinoAjustes());
            conferirSemConflito(destino);
            if (!Set.of("EM_REVISAO", "REJEITADO").contains(destino.getSituacao()))
                throw conflito(
                        "DESTINO_EXIGE_REABERTURA",
                        "Regularização exige composição posterior em revisão.");
            for (var a : faltantes) a.redirecionar(destino);
            ajustes.flush();
            var anterior = versao(destino, destino.getVersaoAtual());
            anterior.situacao("SUPERADA");
            var nova = novaVersao(destino, anterior.getCalculo(), anterior.getNumero() + 1);
            destino.atualizar("EM_REVISAO", nova.getNumero(), agora());
            versoes.flush();
            fechamentos.flush();
            entityManager.lock(
                    destino, jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            destinoAlterado = new ConfirmacaoComando(resposta(destino), resposta(nova));
        } else if (d.destinoAjustesId() != null)
            throw CadastroSupport.invalido("Não há ajuste fora da composição para regularizar.");
        var tratativa =
                new TratativaExternaFechamento(
                        f,
                        alvo,
                        d.resultado(),
                        mapper.writeValueAsString(d.referencias()),
                        texto(d.fonte()),
                        texto(d.conferidaPor()),
                        d.conferidaEm(),
                        agora(),
                        acesso.usuario(),
                        CadastroSupport.motivo(d.motivo()));
        ConfirmacaoComando origemRegularizada = null;
        Ajuste regularizacaoCriada = null;
        if (trocaComDependencias)
            tratativa.identificarBaseAnterior(
                    baseAnterior, mapper.writeValueAsString(dependenciasOrigem));
        tratativas.saveAndFlush(tratativa);
        if (trocaComDependencias) {
            var reg =
                    new AjusteFechamento(
                            alvo,
                            destinoOrigem,
                            alvo.getCalculo(),
                            baseAnterior.getCalculo(),
                            null,
                            operacoes.hash(
                                    "REGULARIZACAO_ORIGEM", tratativa.getId(), dependenciasOrigem),
                            alvo.getCalculo().getTotal(),
                            baseAnterior.getCalculo().getTotal(),
                            deltaOrigem,
                            "VALIDADO",
                            CadastroSupport.motivo(d.motivo()),
                            texto(d.fonte()),
                            acesso.usuario(),
                            agora(),
                            null);
            reg.identificarRegularizacao(tratativa);
            ajustes.saveAndFlush(reg);
            var antiga = versao(destinoOrigem, destinoOrigem.getVersaoAtual());
            antiga.situacao("SUPERADA");
            var nova = novaVersao(destinoOrigem, antiga.getCalculo(), antiga.getNumero() + 1);
            destinoOrigem.atualizar("EM_REVISAO", nova.getNumero(), agora());
            versoes.flush();
            fechamentos.flush();
            entityManager.lock(
                    destinoOrigem, jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            regularizacaoCriada = resposta(reg);
            origemRegularizada = new ConfirmacaoComando(resposta(destinoOrigem), resposta(nova));
        }
        if (d.resultado().equals("EMITIDO")) {
            for (var v : versoes.findByFechamentoIdOrderByNumeroAsc(f.getId()))
                if (!v.getId().equals(alvo.getId()) && !finalizada(v)) v.situacao("SUPERADA");
            alvo.externo("EMITIDO");
            f.atualizar("EMITIDO", alvo.getNumero(), agora());
            // Existing finalization and applied adjustments remain intact; only still-validated
            // ones finalize.
            for (var x : composicoes.findByVersaoIdOrderByIdAsc(alvo.getId()))
                if (x.getAjuste().getSituacao().equals("VALIDADO"))
                    x.getAjuste().aplicar(alvo, agora());
        } else {
            alvo.externo("NAO_EMITIDO_CONFIRMADO");
            String situacao =
                    switch (alvo.getSituacao()) {
                        case "FINALIZADA_SEM_EMISSAO" -> "FINALIZADO_SEM_EMISSAO";
                        case "APROVADA" -> "APROVADO";
                        case "REJEITADA" -> "REJEITADO";
                        case "PENDENTE_REVISAO" -> "EM_REVISAO";
                        default ->
                                throw conflito(
                                        "VERSAO_FINALIZADA",
                                        "Emissão financeira já finalizada exige ajuste posterior, sem reversão silenciosa.");
                    };
            f.atualizar(situacao, alvo.getNumero(), agora());
        }
        ajustes.flush();
        versoes.flush();
        fechamentos.flush();
        entityManager.lock(f, jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        var resultado = new ConfirmacaoComando(resposta(f), resposta(alvo));
        auditoria.registrar(
                "FECHAMENTO_COBRANCA",
                f.getId(),
                "TRATATIVA_EXTERNA",
                CadastroSupport.motivo(d.motivo()),
                new Object[] {antes, atribuicaoAnterior, dependenciasOrigem},
                cadastros.paraAuditoria(
                        d.resolucao(),
                        new Object[] {
                            resultado, destinoAlterado, regularizacaoCriada, origemRegularizada
                        },
                        f.getPeriodoInicio(),
                        f.getPeriodoFim()));
        operacoes.salvar(
                d.operacaoId(),
                "TRATATIVA_EXTERNA",
                f.getCliente(),
                f.getArmazem(),
                f.getId(),
                hash,
                resultado);
        return resultado;
    }

    public List<Tratativa> tratativas(@NotNull @Positive Long id) {
        ler(id);
        return tratativas.findByFechamentoIdOrderByIdAsc(id).stream()
                .map(
                        t ->
                                new Tratativa(
                                        t.getId(),
                                        id,
                                        t.getVersaoResultado().getId(),
                                        t.getResultado(),
                                        mapper.readValue(
                                                t.getReferenciasJson(),
                                                new tools.jackson.core.type.TypeReference<
                                                        List<DocumentoConferido>>() {}),
                                        t.getFonte(),
                                        t.getConferidaPor(),
                                        t.getConferidaEm(),
                                        t.getRegistradaEm(),
                                        t.getUsuario(),
                                        t.getMotivo(),
                                        t.getVersaoBaseAnterior() == null
                                                ? null
                                                : t.getVersaoBaseAnterior().getId(),
                                        t.getDependenciasOrigemJson() == null
                                                ? List.of()
                                                : mapper.readValue(
                                                        t.getDependenciasOrigemJson(),
                                                        new tools.jackson.core.type.TypeReference<
                                                                List<Ajuste>>() {})))
                .toList();
    }

    private List<ReferenciaNfse> documentosConhecidos(FechamentoCobranca f) {
        return versoes.findByFechamentoIdOrderByNumeroAsc(f.getId()).stream()
                .flatMap(v -> documentos.findByVersaoIdOrderByIdAsc(v.getId()).stream())
                .toList();
    }

    public List<Ajuste> ajustes(
            @NotNull @Positive Long clienteId, @NotNull @Positive Long armazemId) {
        contextos.autorizar(clienteId, armazemId);
        return fechamentos
                .findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc(clienteId, armazemId)
                .stream()
                .flatMap(f -> ajustes.findByDestinoFechamentoIdOrderByIdAsc(f.getId()).stream())
                .map(this::resposta)
                .toList();
    }

    private FechamentoCobranca ler(Long id) {
        var escopo = fechamentos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        contextos.autorizar(escopo.getClienteId(), escopo.getArmazemId());
        return fechamentos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private FechamentoCobranca bloquear(Long id, Resolucao resolucao) {
        acesso.exigirGestor();
        var escopo = fechamentos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var ctx = contextos.bloquear(escopo.getClienteId(), escopo.getArmazemId());
        cadastros.conferir(ctx, resolucao);
        var f =
                fechamentos
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        cadastros.conferirPeriodo(
                resolucao,
                f.getPeriodoInicio(),
                f.getPeriodoFim(),
                clock,
                f.getContrato().getFuso());
        return f;
    }

    private FechamentoCobranca destinoRegularizacao(
            FechamentoCobranca origem, Long id, long versao) {
        var candidatos =
                fechamentos
                        .findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc(
                                origem.getCliente().getId(), origem.getArmazem().getId())
                        .stream()
                        .filter(
                                x ->
                                        !x.getPeriodoInicio().isBefore(origem.getPeriodoFim())
                                                && !Set.of("EMITIDO", "FINALIZADO_SEM_EMISSAO")
                                                        .contains(x.getSituacao()))
                        .toList();
        if (candidatos.isEmpty() || !candidatos.getFirst().getId().equals(id))
            throw conflito(
                    "DESTINO_FORA_DA_SEQUENCIA",
                    "Regularizacao pertence ao primeiro ciclo posterior ainda nao finalizado.");
        var destino =
                fechamentos
                        .buscarParaAtualizar(id)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        conferirVersaoLock(destino, versao);
        conferirSemConflito(destino);
        if (!Set.of("EM_REVISAO", "REJEITADO").contains(destino.getSituacao()))
            throw conflito(
                    "DESTINO_EXIGE_REABERTURA", "Destino da regularizacao deve estar em revisao.");
        return destino;
    }

    private CalculoCobranca calculo(Long id) {
        var escopo = calculos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        contextos.autorizar(escopo.getClienteId(), escopo.getArmazemId());
        return calculos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private VersaoFechamento versao(FechamentoCobranca f, int numero) {
        return versoes.findByFechamentoIdAndNumero(f.getId(), numero)
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private ContratoCobranca primeiroContrato(ContextoCobrancaService.Contexto ctx) {
        var cs =
                contratos.findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
                        ctx.cliente().getId(), ctx.armazem().getId());
        if (cs.isEmpty())
            throw conflito("CONTRATO_AUSENTE", "Não há vigência contratual comprovada.");
        return cs.getFirst();
    }

    private ContratoCobranca contrato(ContextoCobrancaService.Contexto ctx, LocalDate inicio) {
        var cs =
                contratos
                        .findByClienteIdAndArmazemIdOrderByVigenciaInicioAsc(
                                ctx.cliente().getId(), ctx.armazem().getId())
                        .stream()
                        .filter(
                                c ->
                                        PeriodoCobrancaService.vigente(
                                                c.getVigenciaInicio(), c.getVigenciaFim(), inicio))
                        .toList();
        if (cs.size() != 1)
            throw conflito(
                    "CONTRATO_AUSENTE_OU_AMBIGUO",
                    "O próximo corte exige configuração única no início do período.");
        return cs.getFirst();
    }

    private void conferirCalculo(FechamentoCobranca f, CalculoCobranca c) {
        if (c.getContrato() == null
                || !f.getCliente().getId().equals(c.getCliente().getId())
                || !f.getArmazem().getId().equals(c.getArmazem().getId())
                || !f.getContrato().getId().equals(c.getContrato().getId())
                || !f.getPeriodoInicio().equals(c.getPeriodoInicio())
                || !f.getPeriodoFim().equals(c.getPeriodoFim()))
            throw conflito(
                    "CONTEXTO_DIVERGENTE",
                    "Cálculo deve ser do contexto/contrato/período original.");
    }

    private void conferirVersao(FechamentoCobranca f, long versao, int numero) {
        conferirVersaoLock(f, versao);
        if (f.getVersaoAtual() != numero)
            throw conflito("VERSAO_SUPERADA", "Comando exige a versão vigente.");
    }

    private void conferirVersaoLock(FechamentoCobranca f, long versao) {
        if (f.getVersao() != versao)
            throw conflito(
                    "VERSAO_DESATUALIZADA", "Consulte o fechamento atual antes de confirmar.");
    }

    private void conferirSemConflito(FechamentoCobranca f) {
        if (f.getSituacao().equals("CONFLITO_EXTERNO"))
            throw conflito(
                    "CONFLITO_EXTERNO",
                    "Emissão externa divergente exige tratativa comprovada fora deste comando.");
    }

    private void exigirAprovada(VersaoFechamento v) {
        if (!v.getSituacao().equals("APROVADA"))
            throw conflito("VERSAO_NAO_APROVADA", "Operação exige aprovação integral vigente.");
    }

    private void conferirReabertura(VersaoFechamento v) {
        if (finalizada(v) || v.getEstadoExterno().equals("EMITIDO"))
            throw conflito(
                    "VERSAO_FINALIZADA", "Correção posterior exige ajuste no próximo ciclo.");
        if (v.getEstadoExterno().equals("DESCONHECIDO"))
            throw conflito(
                    "EMISSAO_DESCONHECIDA",
                    "É necessária declaração externa identificada de não emissão após a última entrega.");
    }

    private boolean aprovadaHistoricamente(VersaoFechamento v) {
        if (Set.of("APROVADA", "EMITIDA", "FINALIZADA_SEM_EMISSAO").contains(v.getSituacao()))
            return true;
        return historicoAuditoria
                .findByTipoAndRegistroIdAndAcaoOrderByIdAsc(
                        "FECHAMENTO_COBRANCA", v.getFechamento().getId(), "APROVACAO_FECHAMENTO")
                .stream()
                .anyMatch(
                        a -> {
                            var registro = mapper.readTree(a.getDadosDepois());
                            var resultado =
                                    registro.has("resolucao")
                                            ? registro.path("resultado")
                                            : registro;
                            return resultado.path("versao").path("id").asLong() == v.getId();
                        });
    }

    private boolean finalizada(VersaoFechamento v) {
        return Set.of("EMITIDA", "FINALIZADA_SEM_EMISSAO").contains(v.getSituacao());
    }

    private void conferirData(Instant data, Instant minimo) {
        if (data.isAfter(agora()) || minimo != null && data.isBefore(minimo))
            throw CadastroSupport.invalido(
                    "Data deve ser comprovada, não futura e posterior ao marco aplicável.");
    }

    private void conferirComposicao(VersaoFechamento v) {
        var atual =
                ajustes.findByDestinoFechamentoIdOrderByIdAsc(v.getFechamento().getId()).stream()
                        .map(AjusteFechamento::getId)
                        .toList();
        var gravada =
                composicoes.findByVersaoIdOrderByIdAsc(v.getId()).stream()
                        .map(x -> x.getAjuste().getId())
                        .toList();
        if (!atual.equals(gravada))
            throw conflito(
                    "COMPOSICAO_DESATUALIZADA",
                    "Ajustes mudaram; nova versão e aprovação integral são necessárias.");
    }

    private void tentarFinalizarSemEmissao(FechamentoCobranca f, VersaoFechamento v) {
        if (v.getEstadoExterno().equals("NAO_EMITIDO_CONFIRMADO")
                && resolucoes.findByVersaoId(v.getId()).isPresent()) {
            apurador.conferirAtual(v.getCalculo());
            conferirComposicao(v);
            v.situacao("FINALIZADA_SEM_EMISSAO");
            f.atualizar("FINALIZADO_SEM_EMISSAO", v.getNumero(), agora());
            aplicar(v);
        }
    }

    private void aplicar(VersaoFechamento v) {
        conferirComposicao(v);
        for (var x : composicoes.findByVersaoIdOrderByIdAsc(v.getId())) {
            var a = x.getAjuste();
            if (!a.getDestinoFechamento().getId().equals(v.getFechamento().getId()))
                throw conflito("CONTEXTO_DIVERGENTE", "Composição aponta outro destino.");
            a.aplicar(v, agora());
        }
        ajustes.flush();
    }

    private VersaoFechamento novaVersao(FechamentoCobranca f, CalculoCobranca calc, int numero) {
        conferirCalculo(f, calc);
        var as = ajustes.findByDestinoFechamentoIdOrderByIdAsc(f.getId());
        BigDecimal saldo =
                calc.getTotal() == null
                        ? null
                        : calc.getTotal()
                                .add(
                                        as.stream()
                                                .map(AjusteFechamento::getDiferenca)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add));
        String natureza =
                saldo == null
                        ? null
                        : saldo.signum() > 0 ? "DEBITO" : saldo.signum() < 0 ? "CREDITO" : "ZERO";
        var c = f.getContrato();
        var ciclo = PeriodoCobrancaService.ciclo(c, f.getPeriodoInicio());
        var demonstrativo =
                new Demonstrativo(
                        1,
                        f.getId(),
                        numero,
                        identificacao(f.getCliente()),
                        identificacao(f.getArmazem()),
                        f.getPeriodoInicio(),
                        f.getPeriodoFim(),
                        new Ciclo(
                                c.getModalidadeCiclo(),
                                c.getVigenciaInicio(),
                                c.getDiaCorte(),
                                c.getDuracaoDias(),
                                ciclo.inicio(),
                                ciclo.fim(),
                                ciclo.dias()),
                        apurador.consultar(calc.getId()),
                        as.stream().map(this::resposta).toList(),
                        saldo,
                        natureza);
        String json = mapper.writeValueAsString(demonstrativo);
        var v =
                versoes.saveAndFlush(
                        new VersaoFechamento(
                                f,
                                calc,
                                numero,
                                "PENDENTE_REVISAO",
                                "NAO_ENVIADO",
                                saldo,
                                natureza,
                                json,
                                OperacaoAdministrativaService.digest(
                                        json.getBytes(StandardCharsets.UTF_8)),
                                agora(),
                                null,
                                null,
                                null));
        for (var a : as) composicoes.save(new AjusteVersaoFechamento(v, a));
        composicoes.flush();
        vincularFatos(f, calc);
        return v;
    }

    private void vincularFatos(FechamentoCobranca f, CalculoCobranca calc) {
        for (var m : apurador.consultar(calc.getId()).memoria().servicos()) {
            var existente = fatos.findByFatoId(m.fatoId()).orElse(null);
            if (existente != null) {
                if (!existente.getFechamento().getId().equals(f.getId()))
                    throw conflito("FATO_JA_FECHADO", "Execução já pertence a outro fechamento.");
            } else {
                var fato =
                        fatosServico
                                .findById(m.fatoId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
                var data = fato.getExecutadoEm().atZone(ZoneId.of(calc.getFuso())).toLocalDate();
                if (!fato.getCliente().getId().equals(f.getCliente().getId())
                        || !fato.getArmazem().getId().equals(f.getArmazem().getId())
                        || data.isBefore(f.getPeriodoInicio())
                        || !data.isBefore(f.getPeriodoFim()))
                    throw conflito(
                            "CONTEXTO_DIVERGENTE",
                            "Fato não pertence ao contexto/período do fechamento.");
                fatos.save(new FatoFechamento(f, fato, agora()));
            }
        }
        fatos.flush();
    }

    private Identificacao identificacao(Cliente c) {
        return new Identificacao(
                c.getId(),
                c.getCodigo(),
                c.getNome(),
                c.getDocumentoFiscal(),
                FiscalCadastroDto.Dados.de(
                        c.getComplementoFiscal(),
                        c.getCidade(),
                        c.getUf(),
                        c.getFaturamentoEmail(),
                        c.getFaturamentoReferencia()));
    }

    private Identificacao identificacao(Armazem a) {
        return new Identificacao(
                a.getId(),
                a.getCodigo(),
                a.getNome(),
                a.getDocumentoFiscal(),
                FiscalCadastroDto.Dados.de(
                        a.getComplementoFiscal(), a.getCidade(), a.getUf(), null, null));
    }

    private ConfirmacaoComando concluir(
            FechamentoCobranca f,
            VersaoFechamento v,
            UUID id,
            String tipo,
            String motivo,
            Object antes,
            String hash,
            Resolucao resolucao) {
        versoes.flush();
        fechamentos.flush();
        entityManager.lock(f, jakarta.persistence.LockModeType.PESSIMISTIC_FORCE_INCREMENT);
        var r = new ConfirmacaoComando(resposta(f), resposta(v));
        auditoria.registrar(
                "FECHAMENTO_COBRANCA",
                f.getId(),
                tipo,
                CadastroSupport.motivo(motivo),
                antes,
                cadastros.paraAuditoria(resolucao, r, f.getPeriodoInicio(), f.getPeriodoFim()));
        operacoes.salvar(id, tipo, f.getCliente(), f.getArmazem(), f.getId(), hash, r);
        return r;
    }

    private Fechamento resposta(FechamentoCobranca f) {
        return new Fechamento(
                f.getId(),
                f.getVersao(),
                f.getCliente().getId(),
                f.getArmazem().getId(),
                f.getContrato().getId(),
                f.getPeriodoInicio(),
                f.getPeriodoFim(),
                f.getSituacao(),
                f.getVersaoAtual(),
                f.getCriadoEm(),
                f.getAlteradoEm());
    }

    private Versao resposta(VersaoFechamento v) {
        return new Versao(
                v.getId(),
                v.getFechamento().getId(),
                v.getNumero(),
                v.getCalculo().getId(),
                v.getSituacao(),
                v.getEstadoExterno(),
                v.getConteudoHash(),
                v.getSaldo(),
                v.getNatureza(),
                v.getCriadaEm(),
                v.getDecididaEm(),
                v.getDecisor(),
                v.getMotivoDecisao(),
                entregas.findByVersaoIdOrderBySequenciaAsc(v.getId()).stream()
                        .map(
                                e ->
                                        new Entrega(
                                                e.getId(),
                                                e.getSequencia(),
                                                e.getLayoutVersao(),
                                                e.getArquivoHash(),
                                                e.getDestinoReferencia(),
                                                e.getEntregueEm(),
                                                e.getRegistradaEm(),
                                                e.getUsuario(),
                                                e.getMotivo()))
                        .toList(),
                confirmacoes.findByVersaoIdOrderByIdAsc(v.getId()).stream()
                        .map(
                                c ->
                                        new Confirmacao(
                                                c.getId(),
                                                c.getEntrega() == null
                                                        ? null
                                                        : c.getEntrega().getId(),
                                                c.getSituacao(),
                                                c.getFonte(),
                                                c.getConfirmadaPor(),
                                                c.getConfirmadaEm(),
                                                c.getRegistradaEm(),
                                                c.getUsuario(),
                                                c.getMotivo()))
                        .toList(),
                documentos.findByVersaoIdOrderByIdAsc(v.getId()).stream()
                        .map(
                                d ->
                                        new Documento(
                                                d.getId(),
                                                d.getEmissorDocumento(),
                                                d.getReferenciaExterna(),
                                                d.getNumero(),
                                                d.getSerie(),
                                                d.getEmitidaEm(),
                                                d.getRegistradaEm(),
                                                d.getFonte(),
                                                d.getConferidaPor(),
                                                d.getUsuario(),
                                                d.getMotivo()))
                        .toList(),
                resolucoes
                        .findByVersaoId(v.getId())
                        .map(
                                r ->
                                        new ResolucaoSaldo(
                                                r.getId(),
                                                r.getTipo(),
                                                r.getReferenciaExterna(),
                                                r.getFonte(),
                                                r.getConfirmadaPor(),
                                                r.getConfirmadaEm(),
                                                r.getRegistradaEm(),
                                                r.getUsuario(),
                                                r.getMotivo()))
                        .orElse(null),
                composicoes.findByVersaoIdOrderByIdAsc(v.getId()).stream()
                        .map(x -> resposta(x.getAjuste()))
                        .toList());
    }

    private Ajuste resposta(AjusteFechamento a) {
        return new Ajuste(
                a.getId(),
                a.getOrigemVersao().getId(),
                a.getDestinoFechamento().getId(),
                a.getCalculoBase().getId(),
                a.getCalculoCorrigido().getId(),
                a.getHashCorrecao(),
                a.getValorBase(),
                a.getValorCorrigido(),
                a.getDiferenca(),
                a.getSituacao(),
                a.getAplicadoVersao() == null ? null : a.getAplicadoVersao().getId(),
                a.getAplicadoEm(),
                a.getMotivo(),
                a.getEvidencia(),
                a.getUsuario(),
                a.getRegistradoEm(),
                a.getTipo(),
                a.getTratativaOrigem() == null ? null : a.getTratativaOrigem().getId());
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    private String texto(String s) {
        var t = CadastroSupport.texto(s);
        if (t.isEmpty())
            throw CadastroSupport.invalido("Referência/fonte/responsável não pode estar vazia.");
        return t;
    }

    private String nulo(String s) {
        return s == null ? null : texto(s);
    }

    private RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }
}
