package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.AvariaDto;
import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.AvariaEstoque;
import br.com.rodogarcia.wms.models.FatoPermanencia;
import br.com.rodogarcia.wms.models.MovimentoEstoque;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.AvariaEstoqueRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.FatoPermanenciaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.SeparacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class AvariaService {
    private final UnidadeLogisticaRepository unidades;
    private final AvariaEstoqueRepository avarias;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final MovimentoEstoqueRepository movimentos;
    private final FatoPermanenciaRepository fatos;
    private final MovimentoSaidaEstoqueService movimento;
    private final ResolucaoCadastroService resolucao;
    private final EstoqueService estoque;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final JsonMapper mapper;
    private final Clock clock;
    private final PedidoSaidaRepository pedidosSaida;
    private final SeparacaoSaidaRepository separacoes;
    private final LinhaTemporalEstoqueService linhaTemporal;

    public AvariaService(
            UnidadeLogisticaRepository unidades,
            AvariaEstoqueRepository avarias,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            MovimentoEstoqueRepository movimentos,
            FatoPermanenciaRepository fatos,
            MovimentoSaidaEstoqueService movimento,
            ResolucaoCadastroService resolucao,
            EstoqueService estoque,
            AcessoService acesso,
            AuditoriaService auditoria,
            JsonMapper mapper,
            Clock clock,
            PedidoSaidaRepository pedidosSaida,
            SeparacaoSaidaRepository separacoes,
            LinhaTemporalEstoqueService linhaTemporal) {
        this.unidades = unidades;
        this.avarias = avarias;
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.movimentos = movimentos;
        this.fatos = fatos;
        this.movimento = movimento;
        this.resolucao = resolucao;
        this.estoque = estoque;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
        this.pedidosSaida = pedidosSaida;
        this.separacoes = separacoes;
        this.linhaTemporal = linhaTemporal;
    }

    public List<AvariaDto.Ocorrencia> listar(@NotNull UUID codigo) {
        var escopo =
                unidades.buscarEscopoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(escopo.getClienteId());
        acesso.armazem(escopo.getArmazemId());
        var u =
                unidades.findByCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        return avarias.findByUnidadeIdOrderById(u.getId()).stream().map(this::detalhe).toList();
    }

    @Transactional
    public AvariaDto.Confirmacao registrar(
            @NotNull UUID codigo, @NotNull @Valid AvariaDto.Registrar d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        acesso.exigirSupervisor();
        var u = bloquear(codigo);
        String hash = hash("AVARIA_DETALHADA", codigo, d);
        var replay = repetida(u, d.operacaoId(), hash);
        if (replay != null) return replay;
        validar(u, d.versaoUnidade(), d.resolverPendentes());
        CadastroSupport.quantidade(u.getProduto(), d.quantidade());
        if (d.ocorridaEm().isAfter(agora()) || d.ocorridaEm().isBefore(u.getChegadaReal()))
            throw CadastroSupport.invalido("Ocorrência fora da permanência física.");
        BigDecimal aberta = avarias.somarAbertas(u.getId());
        if (aberta == null) aberta = BigDecimal.ZERO;
        if (aberta.add(d.quantidade()).compareTo(u.getQuantidade()) > 0)
            throw conflito("AVARIA_EXCEDIDA", "Avarias abertas excedem conteúdo físico.");
        if (aberta.signum() == 0
                && (!u.isCondicaoApta() || u.isAvariaPosterior())
                && d.quantidade().compareTo(u.getQuantidade()) != 0)
            throw CadastroSupport.invalido(
                    "Avaria inicial ou marcador anterior exige tratativa integral da unidade.");
        var antes = estoque.detalhe(u);
        var baseTemporal = linhaTemporal.noInstante(u, d.ocorridaEm());
        var abertas =
                avarias.findByUnidadeIdOrderById(u.getId()).stream()
                        .filter(a -> a.getTratativa().equals("EM_TRATAMENTO"))
                        .toList();
        String ciclo =
                abertas.isEmpty() ? UUID.randomUUID().toString() : abertas.getFirst().getCicloId();
        var a =
                avarias.save(
                        new AvariaEstoque(
                                u,
                                d.quantidade(),
                                baseTemporal.quantidade(),
                                baseTemporal.equivalencia(),
                                ciclo,
                                d.ocorridaEm(),
                                agora(),
                                mapper.writeValueAsString(d.destinos()),
                                CadastroSupport.motivo(d.motivo())));
        u.registrarAvaria(agora());
        movimento.mover(
                u,
                d.destinos(),
                d.conjuntoId(),
                TipoEndereco.QUARENTENA,
                d.resolverPendentes(),
                agora());
        fatos.save(
                new FatoPermanencia(
                        u,
                        d.operacaoId().toString(),
                        "AVARIA",
                        d.ocorridaEm(),
                        agora(),
                        baseTemporal.quantidade(),
                        baseTemporal.quantidade(),
                        baseTemporal.equivalencia(),
                        baseTemporal.equivalencia()));
        return concluir(u, a, antes, d.operacaoId(), hash, "AVARIA_DETALHADA", d.motivo());
    }

    @Transactional
    public AvariaDto.Confirmacao reconhecer(
            @NotNull UUID codigo,
            @NotNull @Positive Long id,
            @NotNull @Valid AvariaDto.Reconhecer d) {
        acesso.exigirGestor();
        var u = bloquear(codigo);
        String hash = hash("RESPONSABILIDADE_AVARIA", codigo, List.of(id, d));
        var replay = repetida(u, d.operacaoId(), hash);
        if (replay != null) return replay;
        var a = obter(u, id);
        if (a.getVersao() != d.versao()) throw conflito("VERSAO_DESATUALIZADA", "Avaria alterada.");
        if (a.getReconhecidaEm() != null)
            throw conflito(
                    "RESPONSABILIDADE_DEFINIDA",
                    "Reconhecimento já registrado; preserve o histórico.");
        var antes = estoque.detalhe(u);
        a.reconhecer(d.responsabilidade(), acesso.usuario(), agora());
        return concluir(u, a, antes, d.operacaoId(), hash, "RESPONSABILIDADE_AVARIA", d.motivo());
    }

    @Transactional
    public AvariaDto.Confirmacao reparar(
            @NotNull UUID codigo, @NotNull @Positive Long id, @NotNull @Valid AvariaDto.Reparar d) {
        if (d.resolverPendentes()) acesso.exigirGestor();
        acesso.exigirSupervisor();
        var u = bloquear(codigo);
        String hash = hash("REPARO_AVARIA", codigo, List.of(id, d));
        var replay = repetida(u, d.operacaoId(), hash);
        if (replay != null) return replay;
        validar(u, d.versaoUnidade(), d.resolverPendentes());
        var a = obter(u, id);
        if (a.getVersao() != d.versao()) throw conflito("VERSAO_DESATUALIZADA", "Avaria alterada.");
        if (!a.getTratativa().equals("EM_TRATAMENTO"))
            throw conflito("SITUACAO_INVALIDA", "Avaria já reparada.");
        var antes = estoque.detalhe(u);
        a.reparar(agora());
        avarias.flush();
        var pendentes =
                avarias.findByUnidadeIdOrderById(u.getId()).stream()
                        .filter(v -> v.getTratativa().equals("EM_TRATAMENTO"))
                        .toList();
        if (pendentes.isEmpty()) {
            u.concluirReparo(agora());
            if (avarias.findByUnidadeIdOrderById(u.getId()).stream()
                    .filter(v -> v.getCicloId().equals(a.getCicloId()))
                    .anyMatch(AvariaEstoque::isBloqueioPrevio)) u.alterarBloqueio(true, agora());
        }
        movimento.mover(
                u,
                d.destinos(),
                d.conjuntoId(),
                pendentes.isEmpty() ? TipoEndereco.ARMAZENAGEM : TipoEndereco.QUARENTENA,
                d.resolverPendentes(),
                agora());
        registrarFato(
                u,
                d.operacaoId(),
                "REPARO",
                agora(),
                BigDecimal.valueOf(antes.posicoesEquivalentes()));
        if (pendentes.isEmpty() && u.getReservaSaida() != null) {
            var p = u.getReservaSaida();
            for (var s : separacoes.buscarDoPedido(p.getId()))
                if (s.getReserva().getUnidade().getId().equals(u.getId())) s.exigirNovaLeitura();
            p.atualizar(SituacaoPedidoSaida.EM_SEPARACAO, agora());
        }
        return concluir(u, a, antes, d.operacaoId(), hash, "REPARO_AVARIA", d.motivo());
    }

    private UnidadeLogistica bloquear(UUID codigo) {
        var e =
                unidades.buscarEscopoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        acesso.cliente(e.getClienteId());
        acesso.armazem(e.getArmazemId());
        clientes.buscarParaAtualizar(e.getClienteId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        armazens.buscarParaAtualizar(e.getArmazemId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        var u =
                unidades.findByCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (u.getReservaSaida() != null)
            pedidosSaida
                    .buscarParaAtualizar(u.getReservaSaida().getId())
                    .orElseThrow(RegraNegocioException::naoEncontrado);
        return u;
    }

    private void validar(UnidadeLogistica u, long v, boolean resolver) {
        if (!u.isAtiva() || u.getPedido().getSituacao() != SituacaoPedidoEntrada.EFETIVADO)
            throw conflito("UNIDADE_ENCERRADA", "Unidade fora do estoque efetivado.");
        if (u.getVersao() != v) throw conflito("VERSAO_DESATUALIZADA", "Unidade alterada.");
        resolucao.conferir(
                resolver,
                u.getPedido().getCliente(),
                u.getPedido().getArmazem(),
                produtos.buscarParaAtualizar(u.getProduto().getId())
                        .orElseThrow(RegraNegocioException::naoEncontrado));
    }

    private AvariaEstoque obter(UnidadeLogistica u, Long id) {
        var a = avarias.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        if (!a.getUnidade().getId().equals(u.getId())) throw RegraNegocioException.naoEncontrado();
        return a;
    }

    private AvariaDto.Ocorrencia detalhe(AvariaEstoque a) {
        boolean rg = a.getResponsabilidade().equals("RODOGARCIA") && a.getReconhecidaEm() != null;
        return new AvariaDto.Ocorrencia(
                a.getId(),
                a.getVersao(),
                a.getUnidade().getId(),
                a.getQuantidade().setScale(6),
                a.getQuantidadeBase().setScale(6),
                a.getEquivalenciaBase().setScale(6),
                a.getCicloId(),
                a.getOcorridaEm(),
                a.getRegistradaEm(),
                a.getResponsabilidade(),
                a.getRelato(),
                a.getReconhecidaEm(),
                a.getValidadaPor(),
                a.getTratativa(),
                a.getResolvidaEm(),
                rg
                        ? a.getQuantidade().divide(a.getQuantidadeBase(), 12, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                rg ? a.getOcorridaEm() : null);
    }

    private AvariaDto.Confirmacao concluir(
            UnidadeLogistica u,
            AvariaEstoque a,
            EstoqueDto.Unidade antes,
            UUID op,
            String hash,
            String tipo,
            String motivo) {
        avarias.flush();
        unidades.flush();
        var registro = detalhe(a);
        var stock = estoque.detalhe(u);
        var salvo =
                new EstoqueDto.Confirmacao(
                        op, u.getPedido().getId(), u.getPedido().getVersao(), stock, registro);
        auditoria.registrar(
                "PEDIDO_ENTRADA",
                u.getPedido().getId(),
                tipo,
                CadastroSupport.motivo(motivo),
                antes,
                salvo);
        movimentos.saveAndFlush(
                new MovimentoEstoque(
                        u.getPedido(),
                        u,
                        op.toString(),
                        hash,
                        tipo,
                        acesso.usuario(),
                        CadastroSupport.motivo(motivo),
                        agora(),
                        mapper.writeValueAsString(antes),
                        mapper.writeValueAsString(salvo)));
        return new AvariaDto.Confirmacao(op, registro, stock);
    }

    private AvariaDto.Confirmacao repetida(UnidadeLogistica u, UUID op, String hash) {
        var a = movimentos.findByPedidoIdAndOperacaoId(u.getPedido().getId(), op.toString());
        if (a.isEmpty()) return null;
        if (!a.get().getConteudoHash().equals(hash))
            throw conflito("OPERACAO_REUTILIZADA", "Identificador já usado com outro conteúdo.");
        var r = mapper.readValue(a.get().getResultado(), EstoqueDto.Confirmacao.class);
        return new AvariaDto.Confirmacao(op, r.avaria(), r.estoque());
    }

    private void registrarFato(
            UnidadeLogistica u, UUID op, String tipo, Instant data, BigDecimal equivalenciaAntes) {
        var equiv = BigDecimal.valueOf(u.getPosicoesEquivalentes());
        fatos.save(
                new FatoPermanencia(
                        u,
                        op.toString(),
                        tipo,
                        data,
                        agora(),
                        u.getQuantidade(),
                        u.getQuantidade(),
                        equivalenciaAntes,
                        equiv));
    }

    private String hash(String tipo, UUID codigo, Object d) {
        return NfeXmlService.hash(tipo + ":" + codigo + ":" + mapper.writeValueAsString(d));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static RegraNegocioException conflito(String c, String m) {
        return RegraNegocioException.conflito(c, m);
    }
}
