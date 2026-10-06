package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.ContagemDto;
import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ContagemEstoque;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.MovimentoEstoque;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.RevisaoContagem;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.SituacaoRevisaoContagem;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ContagemEstoqueRepository;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import br.com.rodogarcia.wms.repositories.RevisaoContagemRepository;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class ContagemEstoqueService {
    private final ContagemEstoqueRepository contagens;
    private final RevisaoContagemRepository revisoes;
    private final UnidadeLogisticaRepository unidades;
    private final ConteudoUnidadeRepository conteudos;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final MovimentoEstoqueRepository movimentos;
    private final FatoPermanenciaRepository permanencias;
    private final ReservaSaidaRepository reservas;
    private final PedidoEntradaService pedidos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final EstoqueService estoque;
    private final AcessoService acesso;
    private final EntityManager em;
    private final JsonMapper mapper;
    private final Clock clock;
    private final HistoricoOperacaoTemporalService historico;
    private final br.com.rodogarcia.wms.repositories.BaixaSaidaRepository baixas;

    public ContagemEstoqueService(
            ContagemEstoqueRepository contagens,
            RevisaoContagemRepository revisoes,
            UnidadeLogisticaRepository unidades,
            ConteudoUnidadeRepository conteudos,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            OcupacaoEnderecoRepository ocupacoes,
            MovimentoEstoqueRepository movimentos,
            FatoPermanenciaRepository permanencias,
            ReservaSaidaRepository reservas,
            PedidoEntradaService pedidos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            EstoqueService estoque,
            AcessoService acesso,
            EntityManager em,
            JsonMapper mapper,
            Clock clock,
            HistoricoOperacaoTemporalService historico,
            br.com.rodogarcia.wms.repositories.BaixaSaidaRepository baixas) {
        this.baixas = baixas;
        this.historico = historico;
        this.contagens = contagens;
        this.revisoes = revisoes;
        this.unidades = unidades;
        this.conteudos = conteudos;
        this.clientes = clientes;
        this.armazens = armazens;
        this.ocupacoes = ocupacoes;
        this.movimentos = movimentos;
        this.permanencias = permanencias;
        this.reservas = reservas;
        this.pedidos = pedidos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.estoque = estoque;
        this.acesso = acesso;
        this.em = em;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public ContagemDto.Resultado contar(@NotNull @Valid ContagemDto.Contar dados) {
        var u = bloquear(dados.codigoUnidade());
        String hash = operacoes.hash("LEITURA_CONTAGEM", dados.codigoUnidade(), dados);
        var repetida = operacoes.repetida(dados.operacaoId(), hash, ContagemDto.Resultado.class);
        if (repetida != null) return repetida;
        if (!u.isAtiva() || u.getVersao() != dados.versaoUnidade())
            throw conflito("VERSAO_DESATUALIZADA", "Consulte o conteúdo ativo antes da leitura.");
        validarQuantidade(u, dados.contado());
        Instant observado = dados.observadoEm().truncatedTo(ChronoUnit.MICROS);
        if (observado.isAfter(agora()) || observado.isBefore(u.getAlteradaEm()))
            throw conflito(
                    "CONTAGEM_TEMPORAL",
                    "A observação deve comprovar o estado atual, após o último movimento e sem data futura.");
        var c =
                contagens
                        .findByUnidadeId(u.getId())
                        .orElseGet(() -> contagens.saveAndFlush(new ContagemEstoque(u, agora())));
        ContagemDto.Resultado antes = c.getRevisaoAtual() == 0 ? null : resultado(c, atual(c));
        if (c.getRevisaoAtual() > 0) {
            var vigente = atual(c);
            if (observado.isBefore(vigente.getObservadoEm())
                    || observado.equals(vigente.getObservadoEm())
                            && dados.contado().compareTo(vigente.getContado()) != 0)
                throw conflito(
                        "CONTAGEM_TEMPORAL",
                        "Leitura anterior ou conflitante no mesmo instante não substitui a revisão vigente.");
            vigente.substituir();
        }
        c.revisar(dados.contado().compareTo(u.getQuantidade()) != 0, agora());
        var origens = origens(u);
        var r =
                revisoes.saveAndFlush(
                        new RevisaoContagem(
                                c,
                                dados.contado(),
                                observado,
                                agora(),
                                acesso.usuario(),
                                CadastroSupport.motivo(dados.motivo()),
                                mapper.writeValueAsString(origens)));
        contagens.flush();
        var resposta = resultado(c, r);
        salvar("LEITURA_CONTAGEM", c, dados.operacaoId(), hash, dados.motivo(), antes, resposta);
        return resposta;
    }

    @Transactional
    public ContagemDto.Resultado aplicar(
            @NotNull @Positive Long id, @NotNull @Valid ContagemDto.Aplicar dados) {
        return aplicarDatado(id, dados, null, null);
    }

    @Transactional
    public ContagemDto.Resultado aplicarContingencia(
            @NotNull @Positive Long id,
            @NotNull @Valid ContagemDto.Aplicar dados,
            @NotNull Instant ocorridaEm,
            @NotNull String identidadeFato) {
        return aplicarDatado(id, dados, ocorridaEm, identidadeFato);
    }

    private ContagemDto.Resultado aplicarDatado(
            Long id, ContagemDto.Aplicar dados, Instant ocorridaEm, String identidadeFato) {
        acesso.exigirSupervisor();
        var escopo = contagens.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        var codigo = UUID.fromString(escopo.getUnidade().getCodigo());
        var u = bloquear(codigo);
        em.refresh(escopo);
        String hash =
                operacoes.hash(
                        "APLICACAO_CONTAGEM",
                        id,
                        ocorridaEm == null ? dados : List.of(dados, ocorridaEm, identidadeFato));
        var repetida = operacoes.repetida(dados.operacaoId(), hash, ContagemDto.Resultado.class);
        if (repetida != null) return repetida;
        var r = atual(escopo);
        if (ocorridaEm != null) {
            historico.conferir(u, ocorridaEm);
            if (ocorridaEm.isBefore(r.getObservadoEm()))
                throw conflito(
                        "HISTORICO_CONTINGENCIA_INSUFICIENTE",
                        "Ajuste deve suceder a observação física comprovada.");
        }
        if (r.getNumero() != dados.revisao()
                || !escopo.isImpedimento()
                || u.getVersao() != dados.versaoUnidade()
                || u.getRevisaoConteudo() != r.getRevisaoConteudo()
                || u.getQuantidade().compareTo(r.getEsperado()) != 0)
            throw conflito(
                    "CONTAGEM_DESATUALIZADA",
                    "Reconte após alteração do conteúdo ou use a revisão atual.");
        if (u.getReservaSaida() != null || u.getTipoLocalizacao() == TipoEndereco.SEPARACAO)
            throw conflito(
                    "PENDENTE_RESERVA",
                    "Concilie o pedido inteiro e seu retorno antes do ajuste; a reserva permanece ativa.");
        if (r.getDiferenca().signum() > 0
                && (!u.isAtiva()
                        || u.getMedidas() == null
                        || ocupacoes.buscarDasUnidades(List.of(u.getId())).size()
                                != u.getMedidas().getPosicoesNecessarias()))
            throw conflito(
                    "RECOMPOSICAO_PENDENTE",
                    "Recomposição exige identidade ativa e capacidade física já comprovadas, além da própria origem não retirada.");
        var composicao = conteudos.buscarOrigens(u.getId());
        var porId = new HashMap<Long, ConteudoUnidade>();
        composicao.forEach(c -> porId.put(c.getEntrada().getId(), c));
        Set<Long> vistos = new HashSet<>();
        BigDecimal soma = BigDecimal.ZERO;
        List<Map<String, Object>> deltas = new ArrayList<>();
        for (var d : dados.origens()) {
            var c = porId.get(d.entradaId());
            if (c == null
                    || !vistos.add(d.entradaId())
                    || d.delta().signum() != 0 && d.delta().signum() != r.getDiferenca().signum()
                    || c.getQuantidade().add(d.delta()).signum() < 0)
                throw CadastroSupport.invalido(
                        "Deltas devem corresponder à diferença e a conteúdo comprovado de cada origem, sem repetição.");
            if (d.delta().signum() > 0) {
                BigDecimal conferida =
                        u.getCondicao() == br.com.rodogarcia.wms.models.CondicaoMercadoria.BOA
                                ? c.getEntrada().getQuantidadeTriagem()
                                : c.getEntrada().getQuantidadeQuarentena();
                BigDecimal restante =
                        conferida
                                .subtract(
                                        conteudos.somarOrigemCondicao(
                                                d.entradaId(), u.getCondicao()))
                                .subtract(
                                        baixas.somarOrigemCondicao(d.entradaId(), u.getCondicao()));
                if (d.delta().compareTo(restante) > 0)
                    throw conflito(
                            "RECOMPOSICAO_PENDENTE",
                            "Origem/condição não comprova saldo restante: conteúdos e retiradas não podem ser duplicados.");
            }
            validarQuantidade(u, d.delta().abs());
            soma = soma.add(d.delta());
            deltas.add(
                    Map.of(
                            "entradaId",
                            d.entradaId(),
                            "itemNotaId",
                            c.getEntrada().getItemChegada().getItemNota().getId(),
                            "quantidade",
                            d.delta().negate(),
                            "antes",
                            c.getQuantidade(),
                            "depois",
                            c.getQuantidade().add(d.delta())));
        }
        if (soma.compareTo(r.getDiferenca()) != 0
                || composicao.stream()
                                .map(ConteudoUnidade::getQuantidade)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .compareTo(u.getQuantidade())
                        != 0)
            throw CadastroSupport.invalido(
                    "Conteúdo e deltas precisam reconciliar exatamente a diferença observada.");
        var antes = resultado(escopo, r);
        var unidadeAntes = estoque.detalhe(u);
        for (var d : dados.origens()) {
            var c = porId.get(d.entradaId());
            c.alterarQuantidade(c.getQuantidade().add(d.delta()));
        }
        var equivalencia = BigDecimal.valueOf(u.getPosicoesEquivalentes());
        var instante = ocorridaEm == null ? agora() : ocorridaEm.truncatedTo(ChronoUnit.MICROS);
        u.alterarQuantidade(r.getContado(), instante);
        if (!u.isAtiva()) {
            ocupacoes.deleteAll(ocupacoes.buscarDasUnidades(List.of(u.getId())));
            u.posicionar(u.getMedidas(), null, null, instante);
        }
        permanencias.save(
                new FatoPermanencia(
                        u,
                        dados.operacaoId().toString(),
                        "AJUSTE_ESTOQUE",
                        instante,
                        instante,
                        r.getEsperado(),
                        r.getContado(),
                        equivalencia,
                        u.isAtiva() ? equivalencia : BigDecimal.ZERO));
        escopo.reconciliar(instante);
        var efeito =
                Map.of(
                        "operacaoId",
                        dados.operacaoId(),
                        "revisaoId",
                        r.getId(),
                        "observadoEm",
                        r.getObservadoEm(),
                        "efetivadoEm",
                        instante,
                        "causa",
                        CadastroSupport.motivo(dados.causa()),
                        "destino",
                        CadastroSupport.motivo(dados.destino()),
                        "comprovacao",
                        CadastroSupport.motivo(dados.comprovacao()),
                        "origens",
                        deltas);
        r.aplicar(instante, mapper.writeValueAsString(efeito));
        conteudos.flush();
        unidades.flush();
        revisoes.flush();
        contagens.flush();
        var p = u.getPedido();
        var pDepois =
                pedidos.registrar(
                        p, PedidoEntradaDto.Resumo.de(p), "AJUSTE_ESTOQUE", dados.motivo(), efeito);
        var confirmacao =
                new EstoqueDto.Confirmacao(
                        dados.operacaoId(), p.getId(), pDepois.versao(), estoque.detalhe(u));
        movimentos.saveAndFlush(
                new MovimentoEstoque(
                        p,
                        u,
                        dados.operacaoId().toString(),
                        hash,
                        "AJUSTE_ESTOQUE",
                        acesso.usuario(),
                        CadastroSupport.motivo(dados.motivo()),
                        instante,
                        mapper.writeValueAsString(unidadeAntes),
                        mapper.writeValueAsString(confirmacao)));
        var resposta = resultado(escopo, r);
        salvar(
                "APLICACAO_CONTAGEM",
                escopo,
                dados.operacaoId(),
                hash,
                dados.motivo(),
                antes,
                resposta);
        return resposta;
    }

    public ContagemDto.Resultado consultar(@NotNull @Positive Long id) {
        var c = obter(id);
        return resultado(c, atual(c));
    }

    public PaginaResponse<ContagemDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @Positive Long produtoId,
            UUID codigo,
            @Positive Long enderecoId,
            ContagemDto.FiltroSituacao situacao,
            Boolean impedimento,
            int pagina,
            int tamanho) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
        return PaginaResponse.de(
                contagens.consultar(
                        clienteId,
                        armazemId,
                        produtoId,
                        codigo == null ? null : codigo.toString(),
                        enderecoId,
                        situacao == null ? null : SituacaoRevisaoContagem.valueOf(situacao.name()),
                        impedimento,
                        CadastroSupport.pagina(pagina, tamanho)),
                c -> resultado(c, atual(c)));
    }

    public PaginaResponse<ContagemDto.Resultado> revisoes(
            @NotNull @Positive Long id, int pagina, int tamanho) {
        return revisoes(id, null, pagina, tamanho);
    }

    public PaginaResponse<ContagemDto.Resultado> revisoes(
            @NotNull @Positive Long id,
            ContagemDto.FiltroSituacao situacao,
            int pagina,
            int tamanho) {
        var c = obter(id);
        return PaginaResponse.de(
                revisoes.consultar(
                        id,
                        situacao == null ? null : SituacaoRevisaoContagem.valueOf(situacao.name()),
                        CadastroSupport.pagina(pagina, tamanho)
                                .withSort(org.springframework.data.domain.Sort.by("numero"))),
                r -> resultado(c, r));
    }

    private ContagemEstoque obter(Long id) {
        var c = contagens.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(c.getUnidade());
        return c;
    }

    private RevisaoContagem atual(ContagemEstoque c) {
        return revisoes.findByContagemIdAndNumero(c.getId(), c.getRevisaoAtual())
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private UnidadeLogistica bloquear(UUID codigo) {
        var pId =
                unidades.buscarPedidoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var p = pedidos.bloquear(pId);
        clientes.buscarParaAtualizar(p.getCliente().getId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        armazens.buscarParaAtualizar(p.getArmazem().getId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        var u =
                unidades.findByCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        em.refresh(u, LockModeType.PESSIMISTIC_WRITE);
        autorizar(u);
        if (p.getSituacao() != SituacaoPedidoEntrada.EFETIVADO)
            throw conflito("SITUACAO_INVALIDA", "Contagem exige entrada efetivada.");
        if (p.getCliente().getSituacao() == SituacaoCadastro.INATIVO
                || p.getArmazem().getSituacao() == SituacaoCadastro.INATIVO
                || u.getProduto().getSituacao() == SituacaoCadastro.INATIVO)
            throw conflito("CADASTRO_INATIVO", "Cadastro inativo não admite compromisso físico.");
        if (p.getCliente().getSituacao() != SituacaoCadastro.ATIVO
                || p.getArmazem().getSituacao() != SituacaoCadastro.ATIVO
                || u.getProduto().getSituacao() != SituacaoCadastro.ATIVO) acesso.exigirGestor();
        return u;
    }

    private void autorizar(UnidadeLogistica u) {
        acesso.cliente(u.getPedido().getCliente().getId());
        acesso.armazem(u.getPedido().getArmazem().getId());
    }

    private List<ContagemDto.Origem> origens(UnidadeLogistica u) {
        return conteudos.buscarOrigens(u.getId()).stream()
                .map(
                        c ->
                                new ContagemDto.Origem(
                                        c.getEntrada().getId(),
                                        c.getEntrada().getItemChegada().getItemNota().getId(),
                                        c.getQuantidade()))
                .toList();
    }

    private ContagemDto.Resultado resultado(ContagemEstoque c, RevisaoContagem r) {
        var u = c.getUnidade();
        BigDecimal reservado =
                reservas.findByUnidadeIdAndSituacao(u.getId(), SituacaoReservaSaida.ATIVA).stream()
                        .map(ReservaSaida::getQuantidade)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ContagemDto.Resultado(
                c.getId(),
                c.getVersao(),
                u.getId(),
                u.getCodigo(),
                r.getNumero(),
                r.getEsperado(),
                r.getContado(),
                r.getDiferenca(),
                reservado,
                r.getObservadoEm(),
                r.getSituacao(),
                c.isImpedimento(),
                mapper.readValue(
                        r.getOrigensJson(), new TypeReference<List<ContagemDto.Origem>>() {}),
                r.getEfeitoJson());
    }

    private void salvar(
            String acao,
            ContagemEstoque c,
            UUID id,
            String hash,
            String motivo,
            Object antes,
            ContagemDto.Resultado depois) {
        var p = c.getUnidade().getPedido();
        auditoria.registrar(
                "CONTAGEM_ESTOQUE", c.getId(), acao, CadastroSupport.motivo(motivo), antes, depois);
        operacoes.salvar(id, acao, p.getCliente(), p.getArmazem(), c.getId(), hash, depois);
    }

    private void validarQuantidade(UnidadeLogistica u, BigDecimal q) {
        if (q.signum() < 0
                || q.stripTrailingZeros().scale() > u.getProduto().getPrecisaoQuantidade())
            throw CadastroSupport.invalido("Quantidade incompatível com a precisão do produto.");
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }
}
