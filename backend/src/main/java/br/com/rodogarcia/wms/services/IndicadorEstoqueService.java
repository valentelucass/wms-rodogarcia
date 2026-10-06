package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.IndicadorEstoqueDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CargaInicial;
import br.com.rodogarcia.wms.models.ConfiguracaoAvisoValidade;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.RevisaoContagem;
import br.com.rodogarcia.wms.models.SituacaoCargaInicial;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ConfiguracaoAvisoValidadeRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class IndicadorEstoqueService {
    private final ProdutoRepository produtos;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ConfiguracaoAvisoValidadeRepository avisos;
    private final EntradaConferidaRepository entradas;
    private final UnidadeLogisticaRepository unidades;
    private final EstoqueService estoque;
    private final CalculoCobrancaService calculos;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final EntityManager em;
    private final Clock clock;

    public IndicadorEstoqueService(
            ProdutoRepository produtos,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ConfiguracaoAvisoValidadeRepository avisos,
            EntradaConferidaRepository entradas,
            UnidadeLogisticaRepository unidades,
            EstoqueService estoque,
            CalculoCobrancaService calculos,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            EntityManager em,
            Clock clock) {
        this.produtos = produtos;
        this.clientes = clientes;
        this.armazens = armazens;
        this.avisos = avisos;
        this.entradas = entradas;
        this.unidades = unidades;
        this.estoque = estoque;
        this.calculos = calculos;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.em = em;
        this.clock = clock;
    }

    @Transactional
    public IndicadorEstoqueDto.Configuracao configurar(
            @NotNull @Valid IndicadorEstoqueDto.ConfigurarAviso d) {
        acesso.exigirGestor();
        autorizar(d.clienteId(), d.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(d.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(d.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        String hash =
                operacoes.hash("CONFIGURACAO_VALIDADE", List.of(d.clienteId(), d.armazemId()), d);
        var replay =
                operacoes.repetida(d.operacaoId(), hash, IndicadorEstoqueDto.Configuracao.class);
        if (replay != null) return replay;
        CadastroSupport.ativo(cliente);
        CadastroSupport.ativo(armazem);
        var c = avisos.findByClienteIdAndArmazemId(d.clienteId(), d.armazemId()).orElse(null);
        var antes = configuracao(d.clienteId(), d.armazemId());
        if (d.versao() != (c == null ? 0 : c.getVersao()))
            throw RegraNegocioException.conflito("VERSAO_DESATUALIZADA", "Consulte o aviso atual.");
        if (c == null)
            c =
                    avisos.save(
                            new ConfiguracaoAvisoValidade(
                                    cliente, armazem, d.diasAntecedencia(), Instant.now(clock)));
        else c.configurar(d.diasAntecedencia(), Instant.now(clock));
        avisos.flush();
        var resposta = configuracao(d.clienteId(), d.armazemId());
        auditoria.registrar(
                "AVISO_VALIDADE",
                c.getId(),
                "CONFIGURACAO_VALIDADE",
                CadastroSupport.motivo(d.motivo()),
                antes,
                resposta);
        operacoes.salvar(
                d.operacaoId(),
                "CONFIGURACAO_VALIDADE",
                cliente,
                armazem,
                c.getId(),
                hash,
                resposta);
        return resposta;
    }

    public IndicadorEstoqueDto.Configuracao configuracao(
            @NotNull @Positive Long clienteId, @NotNull @Positive Long armazemId) {
        autorizar(clienteId, armazemId);
        var c = avisos.findByClienteIdAndArmazemId(clienteId, armazemId).orElse(null);
        return new IndicadorEstoqueDto.Configuracao(
                c == null ? null : c.getId(),
                c == null ? 0 : c.getVersao(),
                clienteId,
                armazemId,
                c == null ? null : c.getDiasAntecedencia(),
                c != null);
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public PaginaResponse<IndicadorEstoqueDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            boolean valor,
            @NotBlank String fuso,
            int pagina,
            int tamanho) {
        return listar(clienteId, armazemId, null, valor, fuso, pagina, tamanho);
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public PaginaResponse<IndicadorEstoqueDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @Positive Long produtoId,
            boolean valor,
            @NotBlank String fuso,
            int pagina,
            int tamanho) {
        autorizar(clienteId, armazemId);
        if (valor) acesso.exigirSupervisor();
        ZoneId zona;
        try {
            zona = ZoneId.of(fuso);
        } catch (RuntimeException e) {
            throw CadastroSupport.invalido("Informe fuso civil válido para a consulta.");
        }
        var config = configuracao(clienteId, armazemId);
        return PaginaResponse.de(
                produtos.consultarIndicadores(
                        clienteId, produtoId, CadastroSupport.pagina(pagina, tamanho)),
                p -> resultado(p, armazemId, valor, zona, config.diasAntecedencia()));
    }

    private IndicadorEstoqueDto.Resultado resultado(
            Produto p, Long armazem, boolean consultarValor, ZoneId zona, Integer antecedencia) {
        Long cliente = p.getCliente().getId();
        var us =
                unidades.historicoCobranca(cliente, armazem).stream()
                        .filter(u -> u.isAtiva() && u.getProduto().getId().equals(p.getId()))
                        .toList();
        var observacoes =
                em.createQuery(
                                "select r from RevisaoContagem r where r.contagem.unidade.produto.id=:produto and r.contagem.unidade.pedido.armazem.id=:armazem and r.contagem.impedimento=true and r.numero=r.contagem.revisaoAtual",
                                RevisaoContagem.class)
                        .setParameter("produto", p.getId())
                        .setParameter("armazem", armazem)
                        .getResultList();
        var cargas =
                em.createQuery(
                                "select c from CargaInicial c where c.produto.id=:produto and c.armazem.id=:armazem and c.situacao in :situacoes",
                                CargaInicial.class)
                        .setParameter("produto", p.getId())
                        .setParameter("armazem", armazem)
                        .setParameter(
                                "situacoes",
                                List.of(
                                        SituacaoCargaInicial.PENDENTE,
                                        SituacaoCargaInicial.PREPARADA))
                        .getResultList();
        var pendencias = new LinkedHashSet<String>();
        var origem = new TreeMap<Long, AcumuladorOrigem>();
        BigDecimal conhecido = BigDecimal.ZERO;
        boolean completo = true;
        if (consultarValor) {
            for (var u : us) {
                var v = calculos.indicadorUnidade(u, Instant.now(clock), zona);
                v.pendencias().forEach(pp -> pendencias.add(pp.codigo() + ":" + u.getCodigo()));
                var c = v.unidade();
                if (c.valorEstoque() == null) completo = false;
                else conhecido = conhecido.add(c.valorEstoque());
                // Origem bruta explicável. Desconto avariado pertence ao agregado da unidade;
                // não distribuir dano desconhecido entre notas com preços distintos.
                for (var o : c.origens())
                    origem.computeIfAbsent(
                                    o.entradaId(),
                                    k ->
                                            new AcumuladorOrigem(
                                                    o.entradaId(), o.notaId(), o.itemNotaId()))
                            .adicionar(
                                    o.quantidade(),
                                    o.valorHistorico(),
                                    u.getId(),
                                    c.valorEstoque() != null
                                            && c.quantidadeAvariada() != null
                                            && c.quantidadeAvariada().signum() == 0);
            }
            for (var e : entradas.historicoCobranca(cliente, armazem)) {
                var i = e.getItemChegada().getItemNota();
                if (!i.getProduto().getId().equals(p.getId()) || e.getUnitizadaEm() != null)
                    continue;
                BigDecimal q = e.getQuantidadeTriagem().add(e.getQuantidadeQuarentena());
                BigDecimal v =
                        i.getValorMercadoria() == null
                                ? null
                                : i.getValorMercadoria()
                                        .multiply(e.getQuantidadeTriagem())
                                        .divide(
                                                i.getQuantidadePrevista(),
                                                12,
                                                RoundingMode.HALF_UP);
                if (v == null) {
                    completo = false;
                    pendencias.add("VALOR_ORIGEM_DESCONHECIDO:" + e.getId());
                } else conhecido = conhecido.add(v);
                origem.computeIfAbsent(
                                e.getId(),
                                k ->
                                        new AcumuladorOrigem(
                                                e.getId(), i.getNota().getId(), i.getId()))
                        .adicionar(q, v, null, v != null);
            }
            if (!cargas.isEmpty()) pendencias.add("ESTAGIO_SEPARADO_DO_VALOR_CONFIRMADO");
        }
        var hoje = Instant.now(clock).atZone(zona).toLocalDate();
        var avisosUnidade =
                us.stream()
                        .filter(
                                u ->
                                        antecedencia != null
                                                && u.getValidade() != null
                                                && !u.getValidade()
                                                        .isAfter(hoje.plusDays(antecedencia)))
                        .map(
                                u ->
                                        new IndicadorEstoqueDto.Aviso(
                                                u.getId(),
                                                u.getCodigo(),
                                                u.getValidade(),
                                                u.getValidade().isBefore(hoje)
                                                        ? "VENCIDA"
                                                        : "PROXIMA"))
                        .toList();
        if (antecedencia == null) pendencias.add("AVISO_VALIDADE_NAO_CONFIGURADO");
        if (!observacoes.isEmpty()) pendencias.add("CONTAGEM_DIVERGENTE");
        return new IndicadorEstoqueDto.Resultado(
                p.getId(),
                p.getSku(),
                estoque.saldo(cliente, armazem, p.getId()),
                cargas.stream()
                        .filter(c -> c.getSituacao() == SituacaoCargaInicial.PENDENTE)
                        .map(CargaInicial::getQuantidade)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                cargas.stream()
                        .filter(c -> c.getSituacao() == SituacaoCargaInicial.PREPARADA)
                        .map(CargaInicial::getQuantidade)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                observacoes.stream()
                        .map(RevisaoContagem::getContado)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                observacoes.stream()
                        .map(RevisaoContagem::getDiferenca)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                consultarValor,
                consultarValor ? conhecido : null,
                consultarValor && completo ? conhecido : null,
                origem.values().stream().map(AcumuladorOrigem::dto).toList(),
                List.copyOf(pendencias),
                avisosUnidade);
    }

    private static final class AcumuladorOrigem {
        final Long entrada, nota, item;
        final Set<Long> unidades = new LinkedHashSet<>();
        BigDecimal quantidade = BigDecimal.ZERO, valor = BigDecimal.ZERO;
        boolean completo = true;

        AcumuladorOrigem(Long entrada, Long nota, Long item) {
            this.entrada = entrada;
            this.nota = nota;
            this.item = item;
        }

        void adicionar(BigDecimal q, BigDecimal v, Long unidade, boolean exato) {
            quantidade = quantidade.add(q);
            if (v != null) valor = valor.add(v);
            completo &= exato;
            if (unidade != null) unidades.add(unidade);
        }

        IndicadorEstoqueDto.ValorOrigem dto() {
            return new IndicadorEstoqueDto.ValorOrigem(
                    entrada, nota, item, quantidade, valor, completo, List.copyOf(unidades));
        }
    }

    private void autorizar(Long cliente, Long armazem) {
        acesso.cliente(cliente);
        acesso.armazem(armazem);
    }
}
