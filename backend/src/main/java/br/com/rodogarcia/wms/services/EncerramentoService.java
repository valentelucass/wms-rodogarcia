package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EncerramentoDto;
import br.com.rodogarcia.wms.dto.EncerramentoDto.Tipo;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.AjusteFechamento;
import br.com.rodogarcia.wms.models.AjusteVersaoFechamento;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.CadastroBase;
import br.com.rodogarcia.wms.models.CalculoCobranca;
import br.com.rodogarcia.wms.models.CargaInicial;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.ContagemEstoque;
import br.com.rodogarcia.wms.models.ContratoCobranca;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.FatoFechamento;
import br.com.rodogarcia.wms.models.FatoServico;
import br.com.rodogarcia.wms.models.FechamentoCobranca;
import br.com.rodogarcia.wms.models.ItemTabelaCobranca;
import br.com.rodogarcia.wms.models.LinhaContingencia;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TabelaCobranca;
import br.com.rodogarcia.wms.models.TratativaExternaFechamento;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.models.VersaoFechamento;
import br.com.rodogarcia.wms.models.VinculoTabelaCliente;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class EncerramentoService {
    private final EntityManager em;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final ResolucaoFinanceiraCadastroService resolucao;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;

    public EncerramentoService(
            EntityManager em,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            ResolucaoFinanceiraCadastroService resolucao,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock) {
        this.em = em;
        this.clientes = clientes;
        this.armazens = armazens;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.resolucao = resolucao;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
    }

    private record Alvo(
            Tipo tipo,
            Long id,
            CadastroBase cadastro,
            ServicoCobranca servico,
            Long cliente,
            Long armazem) {
        long versao() {
            return cadastro == null ? servico.getVersao() : cadastro.getVersao();
        }

        String situacao() {
            return cadastro == null ? servico.getSituacao() : cadastro.getSituacao().name();
        }

        Object entidade() {
            return cadastro == null ? servico : cadastro;
        }

        void situacao(String s, Instant instante) {
            if (cadastro == null) servico.alterarSituacaoEncerramento(s, instante);
            else cadastro.alterarSituacao(SituacaoCadastro.valueOf(s), instante);
        }
    }

    public EncerramentoDto.Resultado consultar(@NotNull Tipo tipo, @NotNull @Positive Long id) {
        acesso.exigirGestor();
        var a = obter(tipo, id);
        return resultado(a);
    }

    @Transactional
    public EncerramentoDto.Resultado solicitar(
            @NotNull Tipo tipo,
            @NotNull @Positive Long id,
            @NotNull @Valid EncerramentoDto.Confirmar d) {
        return mudar(tipo, id, d, false);
    }

    @Transactional
    public EncerramentoDto.Resultado inativar(
            @NotNull Tipo tipo,
            @NotNull @Positive Long id,
            @NotNull @Valid EncerramentoDto.Confirmar d) {
        return mudar(tipo, id, d, true);
    }

    private EncerramentoDto.Resultado mudar(
            Tipo tipo, Long id, EncerramentoDto.Confirmar d, boolean definitivo) {
        acesso.exigirGestor();
        var a = bloquear(tipo, id);
        String acao = definitivo ? "INATIVACAO_DEFINITIVA" : "SOLICITACAO_ENCERRAMENTO";
        String hash = operacoes.hash(acao, List.of(tipo, id), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, EncerramentoDto.Resultado.class);
        if (replay != null) return replay;
        if (a.versao() != d.versao())
            throw conflito(
                    "VERSAO_DESATUALIZADA", "Consulte o cadastro antes de confirmar encerramento.");
        if (!a.situacao().equals(definitivo ? "ENCERRAMENTO_PENDENTE" : "ATIVO"))
            throw conflito(
                    "TRANSICAO_INVALIDA",
                    "Solicitação exige ATIVO; confirmação exige ENCERRAMENTO_PENDENTE, sem reativar INATIVO.");
        var antes = resultado(a);
        if (definitivo && !antes.impedimentos().isEmpty())
            throw conflito(
                    "ENCERRAMENTO_IMPEDIDO",
                    "Há compromissos físicos/financeiros pendentes; consulte os impedimentos identificados.");
        a.situacao(definitivo ? "INATIVO" : "ENCERRAMENTO_PENDENTE", Instant.now(clock));
        em.flush();
        var resposta = resultado(a);
        auditoria.registrar(
                tipo.name(), id, acao, CadastroSupport.motivo(d.motivo()), antes, resposta);
        operacoes.salvar(
                d.operacaoId(),
                acao,
                a.cliente() == null ? null : em.getReference(Cliente.class, a.cliente()),
                a.armazem() == null ? null : em.getReference(Armazem.class, a.armazem()),
                id,
                hash,
                resposta);
        return resposta;
    }

    private Alvo obter(Tipo tipo, Long id) {
        Class<? extends CadastroBase> classe =
                switch (tipo) {
                    case CLIENTE -> Cliente.class;
                    case ARMAZEM -> Armazem.class;
                    case PRODUTO -> Produto.class;
                    case EMBALAGEM -> Embalagem.class;
                    case ENDERECO -> Endereco.class;
                    case CONJUNTO_POSICOES -> ConjuntoPosicoes.class;
                    case SERVICO_COBRANCA -> null;
                };
        var c = classe == null ? null : em.find(classe, id);
        var s = classe == null ? em.find(ServicoCobranca.class, id) : null;
        if (c == null && s == null) throw RegraNegocioException.naoEncontrado();
        Long cliente =
                switch (tipo) {
                    case CLIENTE -> id;
                    case PRODUTO -> ((Produto) c).getCliente().getId();
                    case EMBALAGEM -> ((Embalagem) c).getProduto().getCliente().getId();
                    default -> null;
                };
        Long armazem =
                switch (tipo) {
                    case ARMAZEM -> id;
                    case ENDERECO -> ((Endereco) c).getArmazem().getId();
                    case CONJUNTO_POSICOES -> ((ConjuntoPosicoes) c).getArmazem().getId();
                    default -> null;
                };
        if (cliente != null) acesso.cliente(cliente);
        if (armazem != null) acesso.armazem(armazem);
        return new Alvo(tipo, id, c, s, cliente, armazem);
    }

    private Alvo bloquear(Tipo tipo, Long id) {
        var a = obter(tipo, id);
        if (a.cliente() != null)
            clientes.buscarParaAtualizar(a.cliente())
                    .orElseThrow(RegraNegocioException::naoEncontrado);
        if (a.armazem() != null)
            armazens.buscarParaAtualizar(a.armazem())
                    .orElseThrow(RegraNegocioException::naoEncontrado);
        em.refresh(a.entidade(), LockModeType.PESSIMISTIC_WRITE);
        return a;
    }

    private EncerramentoDto.Resultado resultado(Alvo a) {
        return new EncerramentoDto.Resultado(
                a.tipo(), a.id(), a.versao(), a.situacao(), impedimentos(a));
    }

    private List<EncerramentoDto.Impedimento> impedimentos(Alvo a) {
        var p = new ArrayList<EncerramentoDto.Impedimento>();
        var us =
                em.createQuery(
                                "select u from UnidadeLogistica u where "
                                        + filtroUnidade(a, "u")
                                        + " order by u.id",
                                UnidadeLogistica.class)
                        .setParameter("id", a.id())
                        .getResultList();
        for (var u : us) {
            if (u.isAtiva() && u.getQuantidade().signum() > 0)
                add(
                        p,
                        "SALDO_FISICO",
                        "UNIDADE_LOGISTICA",
                        u.getId(),
                        "Conteúdo positivo precisa de resolução física identificada.");
            if (u.getReservaSaida() != null)
                add(
                        p,
                        "RESERVA_ATIVA",
                        "PEDIDO_SAIDA",
                        u.getReservaSaida().getId(),
                        "Reserva integral permanece vinculada.");
            if (!em.createQuery(
                            "select o.id from OcupacaoEndereco o where o.unidade.id=:id",
                            Long.class)
                    .setParameter("id", u.getId())
                    .getResultList()
                    .isEmpty())
                add(
                        p,
                        "OCUPACAO_ATIVA",
                        "UNIDADE_LOGISTICA",
                        u.getId(),
                        "Há posições físicas ocupadas.");
            for (var c :
                    em.createQuery(
                                    "select c from ContagemEstoque c where c.unidade.id=:id and c.impedimento=true",
                                    ContagemEstoque.class)
                            .setParameter("id", u.getId())
                            .getResultList())
                add(
                        p,
                        "CONTAGEM_PENDENTE",
                        "CONTAGEM_ESTOQUE",
                        c.getId(),
                        "Diferença atual ainda não reconciliada.");
        }
        if (a.tipo() != Tipo.SERVICO_COBRANCA
                && a.tipo() != Tipo.ENDERECO
                && a.tipo() != Tipo.CONJUNTO_POSICOES) {
            String eFiltro = filtroEntrada(a, "e");
            for (var e :
                    em.createQuery(
                                    "select e from EntradaConferida e where e.unitizadaEm is null and "
                                            + eFiltro,
                                    EntradaConferida.class)
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "UNITIZACAO_PENDENTE",
                        "ENTRADA_CONFERIDA",
                        e.getId(),
                        "Físico confirmado ainda não unitizado.");
            String pe = filtroPedido(a, "pe", true);
            String ps = filtroPedido(a, "ps", false);
            for (var peId :
                    em.createQuery(
                                    "select pe.id from PedidoEntrada pe where pe.situacao not in (br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.EFETIVADO,br.com.rodogarcia.wms.models.SituacaoPedidoEntrada.CANCELADO) and "
                                            + pe,
                                    Long.class)
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "ENTRADA_PENDENTE",
                        "PEDIDO_ENTRADA",
                        peId,
                        "Pedido ainda possui compromisso de recebimento/conferência.");
            for (var psId :
                    em.createQuery(
                                    "select ps.id from PedidoSaida ps where ps.situacao not in (br.com.rodogarcia.wms.models.SituacaoPedidoSaida.RETIRADO,br.com.rodogarcia.wms.models.SituacaoPedidoSaida.CANCELADO) and "
                                            + ps,
                                    Long.class)
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "SAIDA_PENDENTE",
                        "PEDIDO_SAIDA",
                        psId,
                        "Pedido integral ainda não concluído/cancelado.");
            String ca =
                    switch (a.tipo()) {
                        case CLIENTE -> "c.cliente.id=:id";
                        case ARMAZEM -> "c.armazem.id=:id";
                        case PRODUTO -> "c.produto.id=:id";
                        case EMBALAGEM ->
                                "exists(select u.id from ConteudoUnidade cu join cu.unidade u where cu.entrada.id=c.entrada.id and u.embalagem.id=:id) or exists(select rc.id from RevisaoCargaInicial rc where rc.carga.id=c.id and c.produto.id=(select emb.produto.id from Embalagem emb where emb.id=:id))";
                        default -> "c.id=:id and 1=0";
                    };
            for (var c :
                    em.createQuery(
                                    "select c from CargaInicial c where c.situacao in (br.com.rodogarcia.wms.models.SituacaoCargaInicial.PENDENTE,br.com.rodogarcia.wms.models.SituacaoCargaInicial.PREPARADA) and ("
                                            + ca
                                            + ")",
                                    CargaInicial.class)
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "CARGA_PENDENTE",
                        "CARGA_INICIAL",
                        c.getId(),
                        "Estágio ou conferência preparada ainda não resolvidos.");
        }
        if (a.tipo() == Tipo.CLIENTE
                || a.tipo() == Tipo.ARMAZEM
                || a.tipo() == Tipo.PRODUTO
                || a.tipo() == Tipo.EMBALAGEM) {
            String filtro = a.tipo() == Tipo.ARMAZEM ? "l.armazem.id=:id" : "l.cliente.id=:id";
            Long contexto =
                    a.tipo() == Tipo.ARMAZEM || a.tipo() == Tipo.CLIENTE ? a.id() : a.cliente();
            for (var l :
                    em.createQuery(
                                    "select l from LinhaContingencia l where l.situacao=br.com.rodogarcia.wms.models.SituacaoContingencia.PENDENTE and "
                                            + filtro,
                                    LinhaContingencia.class)
                            .setParameter("id", contexto)
                            .getResultList())
                add(
                        p,
                        "CONTINGENCIA_PENDENTE",
                        "CONTINGENCIA",
                        l.getId(),
                        "Fato do contexto ainda exige comprovação/conciliação.");
        }
        financeiro(a, us, p);
        return List.copyOf(p.stream().distinct().toList());
    }

    private String filtroUnidade(Alvo a, String u) {
        return switch (a.tipo()) {
            case CLIENTE -> u + ".pedido.cliente.id=:id";
            case ARMAZEM -> u + ".pedido.armazem.id=:id";
            case PRODUTO -> u + ".produto.id=:id";
            case EMBALAGEM -> u + ".embalagem.id=:id";
            case ENDERECO ->
                    "exists(select o.id from OcupacaoEndereco o where o.unidade.id="
                            + u
                            + ".id and o.endereco.id=:id)";
            case CONJUNTO_POSICOES -> u + ".conjuntoAtual.id=:id";
            case SERVICO_COBRANCA -> u + ".id=:id and 1=0";
        };
    }

    private String filtroEntrada(Alvo a, String e) {
        return switch (a.tipo()) {
            case CLIENTE -> e + ".itemChegada.chegada.pedido.cliente.id=:id";
            case ARMAZEM -> e + ".itemChegada.chegada.pedido.armazem.id=:id";
            case PRODUTO -> e + ".itemChegada.itemNota.produto.id=:id";
            case EMBALAGEM ->
                    e
                            + ".itemChegada.itemNota.produto.id=(select emb.produto.id from Embalagem emb where emb.id=:id)";
            default -> e + ".id=:id and 1=0";
        };
    }

    private String filtroPedido(Alvo a, String p, boolean entrada) {
        return switch (a.tipo()) {
            case CLIENTE -> p + ".cliente.id=:id";
            case ARMAZEM -> p + ".armazem.id=:id";
            case PRODUTO, EMBALAGEM ->
                    "exists(select i.id from "
                            + (entrada ? "ItemNotaEntrada" : "ItemPedidoSaida")
                            + " i where "
                            + (entrada ? "i.nota.pedido.id" : "i.pedido.id")
                            + "="
                            + p
                            + ".id and "
                            + (a.tipo() == Tipo.PRODUTO
                                    ? "i.produto.id=:id"
                                    : "i.produto.id=(select emb.produto.id from Embalagem emb where emb.id=:id)")
                            + ")";
            default -> p + ".id=:id and 1=0";
        };
    }

    private void financeiro(
            Alvo a, List<UnidadeLogistica> us, List<EncerramentoDto.Impedimento> p) {
        if (a.tipo() == Tipo.ENDERECO || a.tipo() == Tipo.CONJUNTO_POSICOES) return;
        Set<Long> idsUnidades =
                us.stream()
                        .map(UnidadeLogistica::getId)
                        .collect(java.util.stream.Collectors.toSet());
        String filtro =
                a.tipo() == Tipo.ARMAZEM
                        ? "f.armazem.id=:id"
                        : a.tipo() == Tipo.SERVICO_COBRANCA
                                ? "exists(select fs.id from FatoServico fs where fs.servico.id=:id and fs.cliente.id=f.cliente.id and fs.armazem.id=f.armazem.id) or exists(select it.id from ItemTabelaCobranca it where it.servico.id=:id and it.tabela.armazem.id=f.armazem.id)"
                                : "f.cliente.id=:id";
        Long contexto =
                a.tipo() == Tipo.CLIENTE
                                || a.tipo() == Tipo.ARMAZEM
                                || a.tipo() == Tipo.SERVICO_COBRANCA
                        ? a.id()
                        : a.cliente();
        var fs =
                em.createQuery(
                                "select f from FechamentoCobranca f where ("
                                        + filtro
                                        + ") order by f.id",
                                FechamentoCobranca.class)
                        .setParameter("id", contexto)
                        .getResultList();
        for (var f : fs) {
            var vs =
                    em.createQuery(
                                    "select v from VersaoFechamento v where v.fechamento.id=:id and v.numero=:numero",
                                    VersaoFechamento.class)
                            .setParameter("id", f.getId())
                            .setParameter("numero", f.getVersaoAtual())
                            .getResultList();
            if (vs.size() != 1) {
                add(
                        p,
                        "VERSAO_FINANCEIRA_AUSENTE",
                        "FECHAMENTO_COBRANCA",
                        f.getId(),
                        "Composição atual não comprovada.");
                continue;
            }
            var v = vs.getFirst();
            if (a.tipo() == Tipo.PRODUTO || a.tipo() == Tipo.EMBALAGEM) {
                var memoria = mapper.readTree(v.getCalculo().getMemoriaJson());
                boolean pertinente =
                        memoria.findValues("unidadeId").stream()
                                .anyMatch(
                                        n ->
                                                n.isIntegralNumber()
                                                        && idsUnidades.contains(n.longValue()));
                if (a.tipo() == Tipo.PRODUTO)
                    pertinente |=
                            memoria.findValues("produtoId").stream()
                                    .anyMatch(
                                            n ->
                                                    n.isIntegralNumber()
                                                            && a.id().equals(n.longValue()));
                if (!pertinente) continue;
            }
            if (!finalizado(f, v))
                add(
                        p,
                        "OBRIGACAO_FINANCEIRA_PENDENTE",
                        "FECHAMENTO_COBRANCA",
                        f.getId(),
                        "Fechamento/estado externo/composição atuais ainda não finalizados.");
            for (var aj :
                    em.createQuery(
                                    "select a from AjusteFechamento a where a.destinoFechamento.id=:id or a.origemVersao.fechamento.id=:id",
                                    AjusteFechamento.class)
                            .setParameter("id", f.getId())
                            .getResultList()) {
                if (!"APLICADO".equals(aj.getSituacao())
                        || aj.getAplicadoVersao() == null
                        || !aj.getAplicadoVersao()
                                .getFechamento()
                                .getId()
                                .equals(aj.getDestinoFechamento().getId())
                        || aj.getAplicadoVersao().getNumero()
                                != aj.getDestinoFechamento().getVersaoAtual()
                        || !finalizado(aj.getDestinoFechamento(), aj.getAplicadoVersao())
                        || em.createQuery(
                                                "select count(x) from AjusteVersaoFechamento x where x.versao.id=:versao and x.ajuste.id=:ajuste",
                                                Long.class)
                                        .setParameter("versao", aj.getAplicadoVersao().getId())
                                        .setParameter("ajuste", aj.getId())
                                        .getSingleResult()
                                != 1)
                    add(
                            p,
                            "AJUSTE_FINANCEIRO_PENDENTE",
                            "AJUSTE_FECHAMENTO",
                            aj.getId(),
                            "Delta exige destino/aplicação/composição atual reconciliados.");
            }
        }
        String fatoFiltro =
                switch (a.tipo()) {
                    case CLIENTE -> "ft.cliente.id=:id";
                    case ARMAZEM -> "ft.armazem.id=:id";
                    case PRODUTO -> "ft.produto.id=:id or fu.produto.id=:id";
                    case EMBALAGEM -> "fu.embalagem.id=:id";
                    case SERVICO_COBRANCA -> "ft.servico.id=:id";
                    default -> "ft.id=:id and 1=0";
                };
        for (var fato :
                em.createQuery(
                                "select ft from FatoServico ft left join ft.unidade fu where ft.situacao='CONFIRMADO' and ("
                                        + fatoFiltro
                                        + ")",
                                FatoServico.class)
                        .setParameter("id", a.id())
                        .getResultList()) {
            boolean resolvido =
                    em
                            .createQuery(
                                    "select ff from FatoFechamento ff where ff.fato.id=:id",
                                    FatoFechamento.class)
                            .setParameter("id", fato.getId())
                            .getResultList()
                            .stream()
                            .anyMatch(
                                    ff ->
                                            finalizado(
                                                    ff.getFechamento(),
                                                    versaoAtual(ff.getFechamento())));
            if (!resolvido)
                add(
                        p,
                        "FATO_NAO_FECHADO",
                        "FATO_SERVICO",
                        fato.getId(),
                        "Execução confirmada ainda não compõe obrigação final resolvida.");
        }
        for (var u : us) {
            if (u.getInicioArmazenagemEm() == null) continue;
            var contratos =
                    em.createQuery(
                                    "select c from ContratoCobranca c where c.cliente.id=:cliente and c.armazem.id=:armazem order by c.vigenciaInicio",
                                    ContratoCobranca.class)
                            .setParameter("cliente", u.getPedido().getCliente().getId())
                            .setParameter("armazem", u.getPedido().getArmazem().getId())
                            .getResultList();
            if (contratos.isEmpty()) {
                add(
                        p,
                        "PERMANENCIA_SEM_CONFIGURACAO",
                        "UNIDADE_LOGISTICA",
                        u.getId(),
                        "Fatos de permanência exigem configuração comprovada, cálculo e fechamento.");
                continue;
            }
            ZoneId zona = ZoneId.of(contratos.getFirst().getFuso());
            LocalDate inicio = u.getInicioArmazenagemEm().atZone(zona).toLocalDate();
            LocalDate fim =
                    (u.isAtiva() ? Instant.now(clock) : u.getAlteradaEm())
                            .atZone(zona)
                            .toLocalDate();
            boolean coberto = true;
            for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
                LocalDate d = dia;
                boolean existe =
                        fs.stream()
                                .anyMatch(
                                        f ->
                                                f.getCliente()
                                                                .getId()
                                                                .equals(
                                                                        u.getPedido()
                                                                                .getCliente()
                                                                                .getId())
                                                        && f.getArmazem()
                                                                .getId()
                                                                .equals(
                                                                        u.getPedido()
                                                                                .getArmazem()
                                                                                .getId())
                                                        && !d.isBefore(f.getPeriodoInicio())
                                                        && d.isBefore(f.getPeriodoFim())
                                                        && Set.of(
                                                                        "EMITIDO",
                                                                        "FINALIZADO_SEM_EMISSAO")
                                                                .contains(f.getSituacao())
                                                        && finalizado(f, versaoAtual(f)));
                if (!existe) {
                    coberto = false;
                    break;
                }
            }
            if (!coberto)
                add(
                        p,
                        "PERMANENCIA_NAO_FECHADA",
                        "UNIDADE_LOGISTICA",
                        u.getId(),
                        "Dias físicos/valor até a baixa precisam de fechamento correspondente, sem novo fato fictício.");
        }
        if (a.tipo() == Tipo.CLIENTE || a.tipo() == Tipo.ARMAZEM) {
            String ctx = a.tipo() == Tipo.CLIENTE ? "c.cliente.id=:id" : "c.armazem.id=:id";
            for (var c :
                    em.createQuery(
                                    "select c from ContratoCobranca c where (c.vigenciaFim is null or c.vigenciaFim>:hoje) and "
                                            + ctx,
                                    ContratoCobranca.class)
                            .setParameter("hoje", LocalDate.now(clock))
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "VIGENCIA_CONTRATUAL_ABERTA",
                        "CONTRATO_COBRANCA",
                        c.getId(),
                        "Encerre a vigência futura de forma identificada.");
            String tab = a.tipo() == Tipo.CLIENTE ? "t.cliente.id=:id" : "t.armazem.id=:id";
            String vinc = a.tipo() == Tipo.CLIENTE ? "v.cliente.id=:id" : "v.armazem.id=:id";
            for (var v :
                    em.createQuery(
                                    "select v from VinculoTabelaCliente v where (v.vigenciaFim is null or v.vigenciaFim>:hoje) and "
                                            + vinc,
                                    VinculoTabelaCliente.class)
                            .setParameter("hoje", LocalDate.now(clock))
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "VIGENCIA_VINCULO_ABERTA",
                        "VINCULO_TABELA_CLIENTE",
                        v.getId(),
                        "Encerre o vínculo futuro identificado, preservando as memórias históricas.");
            for (var t :
                    em.createQuery(
                                    "select t from TabelaCobranca t where t.situacao='ATIVA' and (t.vigenciaFim is null or t.vigenciaFim>:hoje) and "
                                            + tab,
                                    TabelaCobranca.class)
                            .setParameter("hoje", LocalDate.now(clock))
                            .setParameter("id", a.id())
                            .getResultList())
                add(
                        p,
                        "VIGENCIA_TABELA_ABERTA",
                        "TABELA_COBRANCA",
                        t.getId(),
                        "Tabela específica/padrão vigente ainda compromete o cadastro.");
        }
        if (a.tipo() == Tipo.SERVICO_COBRANCA)
            for (var cid :
                    em.createQuery(
                                    "select m.contrato.id from ServicoMinimoContrato m where m.servico.id=:id and (m.contrato.vigenciaFim is null or m.contrato.vigenciaFim>:hoje)",
                                    Long.class)
                            .setParameter("id", a.id())
                            .setParameter("hoje", LocalDate.now(clock))
                            .getResultList())
                add(
                        p,
                        "SERVICO_EM_MINIMO_VIGENTE",
                        "CONTRATO_COBRANCA",
                        cid,
                        "Abrangência de mínimo vigente ainda compromete o serviço.");
        if (a.tipo() == Tipo.SERVICO_COBRANCA)
            for (var it :
                    em.createQuery(
                                    "select i from ItemTabelaCobranca i where i.servico.id=:id and i.tabela.situacao='ATIVA' and (i.tabela.vigenciaFim is null or i.tabela.vigenciaFim>:hoje)",
                                    ItemTabelaCobranca.class)
                            .setParameter("id", a.id())
                            .setParameter("hoje", LocalDate.now(clock))
                            .getResultList())
                add(
                        p,
                        "SERVICO_EM_TABELA_VIGENTE",
                        "TABELA_COBRANCA",
                        it.getTabela().getId(),
                        "Encerre vigências futuras antes da inativação do serviço.");
    }

    @Transactional
    public EncerramentoDto.Vigencia encerrarVigencia(
            @NotNull String tipo,
            @NotNull @Positive Long id,
            @NotNull @Valid EncerramentoDto.EncerrarVigencia d) {
        acesso.exigirGestor();
        Object entidade;
        Cliente cliente;
        Armazem armazem;
        LocalDate inicio;
        LocalDate fim;
        long versao;
        if (tipo.equals("CONTRATO")) {
            var c = em.find(ContratoCobranca.class, id);
            if (c == null) throw RegraNegocioException.naoEncontrado();
            entidade = c;
            cliente = c.getCliente();
            armazem = c.getArmazem();
            inicio = c.getVigenciaInicio();
            fim = c.getVigenciaFim();
            versao = c.getVersao();
        } else if (tipo.equals("TABELA")) {
            var t = em.find(TabelaCobranca.class, id);
            if (t == null) throw RegraNegocioException.naoEncontrado();
            entidade = t;
            cliente = t.getCliente();
            armazem = t.getArmazem();
            inicio = t.getVigenciaInicio();
            fim = t.getVigenciaFim();
            versao = t.getVersao();
        } else if (tipo.equals("VINCULO")) {
            var v = em.find(VinculoTabelaCliente.class, id);
            if (v == null) throw RegraNegocioException.naoEncontrado();
            entidade = v;
            cliente = v.getCliente();
            armazem = v.getArmazem();
            inicio = v.getVigenciaInicio();
            fim = v.getVigenciaFim();
            versao = v.getVersao();
        } else throw CadastroSupport.invalido("Tipo de vigência inválido.");
        if (cliente == null) {
            String nome =
                    switch (d.resolucao().tipo()) {
                        case "PEDIDO_ENTRADA" -> "PedidoEntrada";
                        case "PEDIDO_SAIDA" -> "PedidoSaida";
                        case "FECHAMENTO" -> "FechamentoCobranca";
                        default ->
                                throw CadastroSupport.invalido("Referência de resolução inválida.");
                    };
            var referencias =
                    em.createQuery(
                                    "select p.cliente from "
                                            + nome
                                            + " p where p.id=:id and p.armazem.id=:armazem",
                                    Cliente.class)
                            .setParameter("id", d.resolucao().compromissoId())
                            .setParameter("armazem", armazem.getId())
                            .getResultList();
            if (referencias.size() != 1)
                throw conflito(
                        "CONTEXTO_DIVERGENTE",
                        "Referência não comprova o armazém da tabela padrão.");
            cliente = referencias.getFirst();
            var ids = new HashSet<Long>();
            ids.add(cliente.getId());
            for (String nomeContexto :
                    List.of(
                            "VinculoTabelaCliente",
                            "ContratoCobranca",
                            "CalculoCobranca",
                            "FatoServico"))
                ids.addAll(
                        em.createQuery(
                                        "select distinct c.cliente.id from "
                                                + nomeContexto
                                                + " c where c.armazem.id=:id",
                                        Long.class)
                                .setParameter("id", armazem.getId())
                                .getResultList());
            for (Long cid : ids.stream().sorted().toList()) {
                acesso.cliente(cid);
                clientes.buscarParaAtualizar(cid).orElseThrow(RegraNegocioException::naoEncontrado);
            }
        } else {
            acesso.cliente(cliente.getId());
            clientes.buscarParaAtualizar(cliente.getId())
                    .orElseThrow(RegraNegocioException::naoEncontrado);
        }
        acesso.armazem(armazem.getId());
        armazens.buscarParaAtualizar(armazem.getId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        em.refresh(entidade, LockModeType.PESSIMISTIC_WRITE);
        // Referência materializada/revalidada antes de replay, inclusive INATIVO.
        resolucao.conferir(new ContextoCobrancaService.Contexto(cliente, armazem), d.resolucao());
        String hash = operacoes.hash("ENCERRAMENTO_VIGENCIA", List.of(tipo, id), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, EncerramentoDto.Vigencia.class);
        if (replay != null) return replay;
        if (entidade instanceof ContratoCobranca c) {
            versao = c.getVersao();
            inicio = c.getVigenciaInicio();
            fim = c.getVigenciaFim();
        } else if (entidade instanceof TabelaCobranca t) {
            versao = t.getVersao();
            inicio = t.getVigenciaInicio();
            fim = t.getVigenciaFim();
        } else {
            var v = (VinculoTabelaCliente) entidade;
            versao = v.getVersao();
            inicio = v.getVigenciaInicio();
            fim = v.getVigenciaFim();
        }
        if (d.versao() != versao
                || !d.corte().isAfter(inicio)
                || d.corte().isAfter(LocalDate.now(clock))
                || fim != null && d.corte().isAfter(fim))
            throw conflito(
                    "CORTE_VIGENCIA_INVALIDO",
                    "Corte deve encerrar trecho fornecido já existente, sem prorrogar vigência.");
        conferirCorte(entidade, d.corte());
        var antes = new EncerramentoDto.Vigencia(tipo, id, versao, inicio, fim);
        if (entidade instanceof ContratoCobranca c) c.encerrar(d.corte(), Instant.now(clock));
        else if (entidade instanceof TabelaCobranca t) t.encerrar(d.corte(), Instant.now(clock));
        else ((VinculoTabelaCliente) entidade).encerrar(d.corte(), Instant.now(clock));
        em.flush();
        long nova =
                entidade instanceof ContratoCobranca c
                        ? c.getVersao()
                        : entidade instanceof TabelaCobranca t
                                ? t.getVersao()
                                : ((VinculoTabelaCliente) entidade).getVersao();
        var resposta = new EncerramentoDto.Vigencia(tipo, id, nova, inicio, d.corte());
        auditoria.registrar(
                tipo.equals("CONTRATO") ? "CONTRATO_COBRANCA" : "TABELA_COBRANCA",
                id,
                "ENCERRAMENTO_VIGENCIA",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resolucao.paraAuditoria(d.resolucao(), resposta));
        operacoes.salvar(
                d.operacaoId(), "ENCERRAMENTO_VIGENCIA", cliente, armazem, id, hash, resposta);
        return resposta;
    }

    private VersaoFechamento versaoAtual(FechamentoCobranca f) {
        var vs =
                em.createQuery(
                                "select v from VersaoFechamento v where v.fechamento.id=:id and v.numero=:numero",
                                VersaoFechamento.class)
                        .setParameter("id", f.getId())
                        .setParameter("numero", f.getVersaoAtual())
                        .getResultList();
        return vs.size() == 1 ? vs.getFirst() : null;
    }

    private boolean finalizado(FechamentoCobranca f, VersaoFechamento v) {
        if (v == null
                || v.getNumero() != f.getVersaoAtual()
                || !v.getFechamento().getId().equals(f.getId())) return false;
        boolean semEmissao =
                "FINALIZADO_SEM_EMISSAO".equals(f.getSituacao())
                        && "FINALIZADA_SEM_EMISSAO".equals(v.getSituacao())
                        && "NAO_EMITIDO_CONFIRMADO".equals(v.getEstadoExterno());
        boolean emitido =
                "EMITIDO".equals(f.getSituacao())
                        && "EMITIDO".equals(v.getEstadoExterno())
                        && "EMITIDA".equals(v.getSituacao());
        if ("EMITIDO".equals(f.getSituacao())
                && "EMITIDO".equals(v.getEstadoExterno())
                && "SUPERADA".equals(v.getSituacao())) {
            var ts =
                    em.createQuery(
                                    "select t from TratativaExternaFechamento t where t.fechamento.id=:id order by t.id desc",
                                    TratativaExternaFechamento.class)
                            .setParameter("id", f.getId())
                            .getResultList();
            emitido =
                    !ts.isEmpty()
                            && "EMITIDO".equals(ts.getFirst().getResultado())
                            && ts.getFirst().getVersaoResultado().getId().equals(v.getId())
                            && ts.getFirst().getFonte() != null
                            && ts.getFirst().getConferidaPor() != null
                            && ts.getFirst().getConferidaEm() != null;
        }
        if (!semEmissao && !emitido) return false;
        for (var x :
                em.createQuery(
                                "select x from AjusteVersaoFechamento x where x.versao.id=:id",
                                AjusteVersaoFechamento.class)
                        .setParameter("id", v.getId())
                        .getResultList()) {
            var a = x.getAjuste();
            if (!"APLICADO".equals(a.getSituacao())
                    || a.getAplicadoVersao() == null
                    || !a.getDestinoFechamento().getId().equals(f.getId())
                    || !a.getAplicadoVersao().getId().equals(v.getId())) return false;
        }
        return true;
    }

    private void conferirCorte(Object entidade, LocalDate corte) {
        Long armazem =
                entidade instanceof ContratoCobranca c
                        ? c.getArmazem().getId()
                        : entidade instanceof TabelaCobranca t
                                ? t.getArmazem().getId()
                                : ((VinculoTabelaCliente) entidade).getArmazem().getId();
        for (var calc :
                em.createQuery(
                                "select c from CalculoCobranca c where c.armazem.id=:armazem and c.periodoFim>:corte",
                                CalculoCobranca.class)
                        .setParameter("armazem", armazem)
                        .setParameter("corte", corte)
                        .getResultList()) {
            if (entidade instanceof ContratoCobranca c) {
                if (calc.getContrato() != null && calc.getContrato().getId().equals(c.getId()))
                    vigenciaComprometida();
                continue;
            }
            if (entidade instanceof VinculoTabelaCliente v
                    && !v.getCliente().getId().equals(calc.getCliente().getId())) continue;
            Long tabela =
                    entidade instanceof TabelaCobranca t
                            ? t.getId()
                            : ((VinculoTabelaCliente) entidade).getTabela().getId();
            var m =
                    mapper.readValue(
                            calc.getMemoriaJson(),
                            br.com.rodogarcia.wms.dto.CalculoCobrancaDto.Memoria.class);
            for (var dia : m.diarias())
                if (!dia.data().isBefore(corte)
                        && (tabela.equals(dia.tabelaId())
                                || dia.regras().stream()
                                        .anyMatch(r -> tabela.equals(r.tabelaId()))))
                    vigenciaComprometida();
            for (var s : m.servicos())
                if (tabela.equals(s.tabelaId())
                        && !s.executadoEm()
                                .atZone(ZoneId.of(calc.getFuso()))
                                .toLocalDate()
                                .isBefore(corte)) vigenciaComprometida();
        }
        for (var fato :
                em.createQuery(
                                "select f from FatoServico f where f.armazem.id=:id and f.situacao='CONFIRMADO'",
                                FatoServico.class)
                        .setParameter("id", armazem)
                        .getResultList()) {
            if (entidade instanceof ContratoCobranca c) {
                var data = fato.getExecutadoEm().atZone(ZoneId.of(c.getFuso())).toLocalDate();
                if (c.getCliente().getId().equals(fato.getCliente().getId())
                        && !data.isBefore(corte)
                        && PeriodoCobrancaService.vigente(
                                c.getVigenciaInicio(), c.getVigenciaFim(), data))
                    vigenciaComprometida();
            } else
                for (var vinculo :
                        em.createQuery(
                                        "select v from VinculoTabelaCliente v where v.cliente.id=:cliente and v.armazem.id=:armazem",
                                        VinculoTabelaCliente.class)
                                .setParameter("cliente", fato.getCliente().getId())
                                .setParameter("armazem", armazem)
                                .getResultList()) {
                    boolean depende =
                            entidade instanceof TabelaCobranca t
                                    ? vinculo.getTabela().getId().equals(t.getId())
                                    : vinculo.getId()
                                            .equals(((VinculoTabelaCliente) entidade).getId());
                    if (!depende) continue;
                    var contratos =
                            em.createQuery(
                                            "select c from ContratoCobranca c where c.cliente.id=:cliente and c.armazem.id=:armazem",
                                            ContratoCobranca.class)
                                    .setParameter("cliente", fato.getCliente().getId())
                                    .setParameter("armazem", armazem)
                                    .getResultList();
                    if (contratos.isEmpty())
                        vigenciaComprometida(); // Sem fuso comprovado não presumir corte seguro.
                    for (var c : contratos) {
                        var data =
                                fato.getExecutadoEm().atZone(ZoneId.of(c.getFuso())).toLocalDate();
                        if (!data.isBefore(corte)
                                && PeriodoCobrancaService.vigente(
                                        c.getVigenciaInicio(), c.getVigenciaFim(), data)
                                && PeriodoCobrancaService.vigente(
                                        vinculo.getVigenciaInicio(),
                                        vinculo.getVigenciaFim(),
                                        data)) vigenciaComprometida();
                    }
                }
        }
    }

    private static void vigenciaComprometida() {
        throw conflito(
                "VIGENCIA_COMPROMETIDA",
                "Há fatos ou memórias dependentes no trecho do corte; correção retrospectiva exige ajuste BE13, preservando configuração e snapshots.");
    }

    private static void add(
            List<EncerramentoDto.Impedimento> p,
            String codigo,
            String recurso,
            Long id,
            String detalhe) {
        p.add(new EncerramentoDto.Impedimento(codigo, recurso, id, detalhe));
    }

    private static RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }
}
