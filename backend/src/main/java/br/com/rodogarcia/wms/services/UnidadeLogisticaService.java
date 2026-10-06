package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.Embalagem;
import br.com.rodogarcia.wms.models.OperacaoUnidade;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.EmbalagemRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoUnidadeRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
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
import java.util.Objects;
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
public class UnidadeLogisticaService {
    private final PedidoEntradaService pedidos;
    private final EntradaConferidaRepository entradas;
    private final UnidadeLogisticaRepository unidades;
    private final ConteudoUnidadeRepository conteudos;
    private final OperacaoUnidadeRepository operacoes;
    private final EmbalagemRepository embalagens;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;
    private final EstoqueService estoque;

    public UnidadeLogisticaService(
            PedidoEntradaService pedidos,
            EntradaConferidaRepository entradas,
            UnidadeLogisticaRepository unidades,
            ConteudoUnidadeRepository conteudos,
            OperacaoUnidadeRepository operacoes,
            EmbalagemRepository embalagens,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock,
            EstoqueService estoque) {
        this.pedidos = pedidos;
        this.entradas = entradas;
        this.unidades = unidades;
        this.conteudos = conteudos;
        this.operacoes = operacoes;
        this.embalagens = embalagens;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
        this.estoque = estoque;
    }

    @Transactional
    public UnidadeLogisticaDto.Resultado unitizar(
            @NotNull @Positive Long pedidoId,
            @NotNull @Positive Long entradaId,
            @NotNull @Valid UnidadeLogisticaDto.Unitizar dados) {
        var p = pedidos.bloquear(pedidoId);
        String hash = hash("UNITIZAR", entradaId, dados);
        var repetida = repetida(pedidoId, dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        PedidoEntradaService.versao(p, dados.versaoPedido());
        validarPedido(p);
        var embalagensPorId =
                bloquearEmbalagens(
                        dados.unidades().stream()
                                .map(UnidadeLogisticaDto.NovaUnidade::embalagemId)
                                .toList());
        var entrada =
                entradas.findByIdAndItemChegadaChegadaPedidoId(entradaId, pedidoId)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (entrada.getUnitizadaEm() != null)
            throw RegraNegocioException.conflito(
                    "ENTRADA_JA_UNITIZADA", "Esta entrada já foi unitizada.");
        var produto = entrada.getItemChegada().getItemNota().getProduto();
        BigDecimal boas = BigDecimal.ZERO;
        BigDecimal avariadas = BigDecimal.ZERO;
        for (var nova : dados.unidades()) {
            CadastroSupport.quantidade(produto, nova.quantidade());
            if (!embalagensPorId
                    .get(nova.embalagemId())
                    .getProduto()
                    .getId()
                    .equals(produto.getId()))
                throw CadastroSupport.invalido("A embalagem deve pertencer ao produto da entrada.");
            if (nova.condicao() == CondicaoMercadoria.BOA) boas = boas.add(nova.quantidade());
            else avariadas = avariadas.add(nova.quantidade());
        }
        if (boas.compareTo(entrada.getQuantidadeTriagem()) != 0
                || avariadas.compareTo(entrada.getQuantidadeQuarentena()) != 0)
            throw RegraNegocioException.conflito(
                    "QUANTIDADE_DIVERGENTE",
                    "A soma das unidades deve corresponder ao conferido, separando boas e avariadas.");
        limiteUnidades(pedidoId, dados.unidades().size());
        var antes = PedidoEntradaDto.Resumo.de(p);
        Instant agora = agora();
        var criadas = new ArrayList<UnidadeLogistica>();
        for (var nova : dados.unidades()) {
            var unidade =
                    unidades.save(
                            new UnidadeLogistica(
                                    entrada,
                                    embalagensPorId.get(nova.embalagemId()),
                                    nova.tipo(),
                                    nova.condicao(),
                                    nova.quantidade(),
                                    agora));
            conteudos.save(new ConteudoUnidade(unidade, entrada, nova.quantidade()));
            criadas.add(unidade);
        }
        entrada.marcarUnitizada(agora);
        return concluir(
                p,
                antes,
                dados.operacaoId(),
                hash,
                "UNIDADES_CRIADAS",
                dados.motivo(),
                List.of(),
                criadas);
    }

    @Transactional
    public UnidadeLogisticaDto.Resultado dividir(
            @NotNull @Positive Long pedidoId,
            @NotNull @Positive Long unidadeId,
            @NotNull @Valid UnidadeLogisticaDto.Dividir dados) {
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(pedidoId);
        String hash = hash("DIVIDIR", unidadeId, dados);
        var repetida = repetida(pedidoId, dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        validarPedido(p);
        bloquearEmbalagens(unidades.buscarEmbalagens(pedidoId, List.of(unidadeId)));
        var origem = obter(pedidoId, unidadeId);
        validarRevisao(origem, dados.versao());
        CadastroSupport.quantidade(origem.getProduto(), dados.quantidadeNovaUnidade());
        if (dados.quantidadeNovaUnidade().compareTo(origem.getQuantidade()) >= 0)
            throw CadastroSupport.invalido(
                    "A divisão precisa deixar quantidade positiva na unidade original.");
        limiteUnidades(pedidoId, 1);
        var conteudoOrigem = conteudos.buscarOrigens(unidadeId);
        conferirConteudo(origem, conteudoOrigem);
        var antes = PedidoEntradaDto.Resumo.de(p);
        var unidadesAntes = List.of(detalhe(origem));
        Instant agora = agora();
        var nova =
                unidades.save(new UnidadeLogistica(origem, dados.quantidadeNovaUnidade(), agora));
        BigDecimal restante = dados.quantidadeNovaUnidade();
        // Ordem estável por entrada: transfere a composição sem inventar uma origem.
        for (var conteudo : conteudoOrigem) {
            BigDecimal transferida = restante.min(conteudo.getQuantidade());
            if (transferida.signum() > 0) {
                conteudos.save(new ConteudoUnidade(nova, conteudo.getEntrada(), transferida));
                conteudo.alterarQuantidade(conteudo.getQuantidade().subtract(transferida));
                restante = restante.subtract(transferida);
            }
        }
        origem.alterarQuantidade(
                origem.getQuantidade().subtract(dados.quantidadeNovaUnidade()), agora);
        return concluir(
                p,
                antes,
                dados.operacaoId(),
                hash,
                "UNIDADE_DIVIDIDA",
                dados.motivo(),
                unidadesAntes,
                List.of(origem, nova));
    }

    @Transactional
    public UnidadeLogisticaDto.Resultado reagrupar(
            @NotNull @Positive Long pedidoId,
            @NotNull @Positive Long destinoId,
            @NotNull @Valid UnidadeLogisticaDto.Reagrupar dados) {
        acesso.exigirSupervisor();
        var p = pedidos.bloquear(pedidoId);
        String hash = hash("REAGRUPAR", destinoId, dados);
        var repetida = repetida(pedidoId, dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        validarPedido(p);
        var ids = new HashSet<Long>();
        ids.add(destinoId);
        for (var revisao : dados.origens()) {
            if (!ids.add(revisao.unidadeId()))
                throw CadastroSupport.invalido(
                        "Informe unidades de origem distintas e diferentes do destino.");
        }
        bloquearEmbalagens(unidades.buscarEmbalagens(pedidoId, ids.stream().sorted().toList()));
        var destino = obter(pedidoId, destinoId);
        validarRevisao(destino, dados.versaoDestino());
        var envolvidas = new ArrayList<UnidadeLogistica>();
        envolvidas.add(destino);
        BigDecimal total = destino.getQuantidade();
        for (var revisao : dados.origens()) {
            var origem = obter(pedidoId, revisao.unidadeId());
            validarRevisao(origem, revisao.versao());
            validarCompatibilidade(destino, origem);
            total = total.add(origem.getQuantidade());
            envolvidas.add(origem);
        }
        if (total.stripTrailingZeros().precision() - total.stripTrailingZeros().scale() > 13)
            throw CadastroSupport.invalido("Quantidade total excede o limite da unidade.");
        var conteudoPorUnidade = new HashMap<Long, List<ConteudoUnidade>>();
        for (var u : envolvidas) {
            var composicao = conteudos.buscarOrigens(u.getId());
            conferirConteudo(u, composicao);
            conteudoPorUnidade.put(u.getId(), composicao);
        }
        var destinoPorEntrada = new HashMap<Long, ConteudoUnidade>();
        for (var c : conteudoPorUnidade.get(destinoId))
            destinoPorEntrada.put(c.getEntrada().getId(), c);
        var origensResultantes = new HashSet<>(destinoPorEntrada.keySet());
        conteudoPorUnidade.values().stream()
                .flatMap(List::stream)
                .filter(c -> c.getQuantidade().signum() > 0)
                .forEach(c -> origensResultantes.add(c.getEntrada().getId()));
        if (origensResultantes.size() > 100)
            throw CadastroSupport.invalido("Uma unidade pode manter até 100 vínculos de origem.");
        var antes = PedidoEntradaDto.Resumo.de(p);
        var unidadesAntes = envolvidas.stream().map(this::detalhe).toList();
        Instant agora = agora();
        for (var origem : envolvidas.subList(1, envolvidas.size())) {
            for (var c : conteudoPorUnidade.get(origem.getId())) {
                if (c.getQuantidade().signum() == 0) continue;
                var existente = destinoPorEntrada.get(c.getEntrada().getId());
                if (existente == null) {
                    existente =
                            conteudos.save(
                                    new ConteudoUnidade(
                                            destino, c.getEntrada(), c.getQuantidade()));
                    destinoPorEntrada.put(c.getEntrada().getId(), existente);
                } else
                    existente.alterarQuantidade(existente.getQuantidade().add(c.getQuantidade()));
                c.alterarQuantidade(BigDecimal.ZERO);
            }
            origem.alterarQuantidade(BigDecimal.ZERO, agora);
        }
        destino.alterarQuantidade(total, agora);
        return concluir(
                p,
                antes,
                dados.operacaoId(),
                hash,
                "UNIDADES_REAGRUPADAS",
                dados.motivo(),
                unidadesAntes,
                envolvidas);
    }

    public PaginaResponse<UnidadeLogisticaDto.Resumo> listar(
            @NotNull @Positive Long pedidoId, int pagina, int tamanho) {
        pedidos.obter(pedidoId);
        var resultado = unidades.findByPedidoId(pedidoId, CadastroSupport.pagina(pagina, tamanho));
        var disponiveis =
                estoque.disponiveis(
                        resultado.getContent().stream().map(UnidadeLogistica::getId).toList());
        return PaginaResponse.de(
                resultado, u -> UnidadeLogisticaDto.Resumo.de(u, disponiveis.contains(u.getId())));
    }

    public UnidadeLogisticaDto.Detalhe consultar(
            @NotNull @Positive Long pedidoId, @NotNull @Positive Long unidadeId) {
        pedidos.obter(pedidoId);
        return detalhe(obter(pedidoId, unidadeId));
    }

    public UnidadeLogisticaDto.Detalhe lerCodigo(@NotNull UUID codigo) {
        return detalhe(obterPorCodigo(codigo));
    }

    public UnidadeLogisticaDto.Etiqueta etiqueta(@NotNull UUID codigo) {
        var unidade = obterPorCodigo(codigo);
        if (!unidade.isAtiva())
            throw RegraNegocioException.conflito(
                    "UNIDADE_ENCERRADA",
                    "Unidade encerrada por reagrupamento não permite nova etiqueta.");
        return UnidadeLogisticaDto.Etiqueta.de(unidade);
    }

    public UnidadeLogisticaDto.Progresso progresso(@NotNull @Positive Long pedidoId) {
        var p = pedidos.obter(pedidoId);
        long total = entradas.countByItemChegadaChegadaPedidoId(pedidoId);
        long unitizadas =
                entradas.countByItemChegadaChegadaPedidoIdAndUnitizadaEmIsNotNull(pedidoId);
        return new UnidadeLogisticaDto.Progresso(
                pedidoId,
                total,
                unitizadas,
                total - unitizadas,
                p.getSituacao() == SituacaoPedidoEntrada.EFETIVADO
                        && total > 0
                        && total == unitizadas);
    }

    private UnidadeLogistica obterPorCodigo(UUID codigo) {
        Long pedidoId =
                unidades.buscarPedidoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        pedidos.obter(pedidoId);
        return unidades.findByCodigo(codigo.toString())
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private UnidadeLogistica obter(Long pedidoId, Long unidadeId) {
        return unidades.findByIdAndPedidoId(unidadeId, pedidoId)
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private void validarPedido(PedidoEntrada p) {
        PedidoEntradaService.exigirSituacao(p, SituacaoPedidoEntrada.EFETIVADO);
        pedidos.validarVinculos(p);
    }

    private Map<Long, Embalagem> bloquearEmbalagens(List<Long> ids) {
        var resultado = new HashMap<Long, Embalagem>();
        // Antes de carregar unidades/entradas: pedido -> cliente -> armazém -> produtos ->
        // embalagens.
        for (Long id : ids.stream().distinct().sorted().toList()) {
            var embalagem =
                    embalagens
                            .buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            CadastroSupport.ativo(embalagem);
            resultado.put(id, embalagem);
        }
        return resultado;
    }

    private void validarRevisao(UnidadeLogistica u, long versao) {
        if (u.getPrimeiroEnderecamentoEm() != null || u.isBloqueada())
            throw RegraNegocioException.conflito(
                    "TRANSFORMACAO_BLOQUEADA",
                    "Unidade posicionada ou bloqueada exige tratamento próprio antes de transformar seu conteúdo.");
        if (u.getVersao() != versao)
            throw RegraNegocioException.conflito(
                    "VERSAO_DESATUALIZADA",
                    "Unidade alterada. Consulte novamente antes de confirmar.");
        if (!u.isAtiva())
            throw RegraNegocioException.conflito(
                    "UNIDADE_ENCERRADA", "Unidade encerrada por reagrupamento.");
    }

    private void validarCompatibilidade(UnidadeLogistica destino, UnidadeLogistica origem) {
        if (!destino.getNota().getId().equals(origem.getNota().getId())
                || !destino.getProduto().getId().equals(origem.getProduto().getId())
                || !destino.getEmbalagem().getId().equals(origem.getEmbalagem().getId())
                || destino.getTipo() != origem.getTipo()
                || destino.getCondicao() != origem.getCondicao()
                || !Objects.equals(destino.getLote(), origem.getLote())
                || !Objects.equals(destino.getValidade(), origem.getValidade())
                || !destino.getDataFifo().equals(origem.getDataFifo())
                || !destino.getChegadaReal().equals(origem.getChegadaReal()))
            throw RegraNegocioException.conflito(
                    "ORIGENS_INCOMPATIVEIS",
                    "Reagrupamento exige mesma nota, produto, lote, validade, FIFO, chegada, embalagem, tipo e condição.");
    }

    private void conferirConteudo(UnidadeLogistica unidade, List<ConteudoUnidade> composicao) {
        var soma =
                composicao.stream()
                        .map(ConteudoUnidade::getQuantidade)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (soma.compareTo(unidade.getQuantidade()) != 0)
            throw RegraNegocioException.conflito(
                    "ORIGEM_INCONSISTENTE",
                    "Composição da unidade exige conciliação antes de movimentar.");
    }

    private void limiteUnidades(Long pedidoId, int adicionais) {
        if (unidades.countByPedidoId(pedidoId) + adicionais > 10000)
            throw CadastroSupport.invalido("Limite de 10000 identidades por pedido atingido.");
    }

    private UnidadeLogisticaDto.Detalhe detalhe(UnidadeLogistica unidade) {
        var origens =
                conteudos.buscarOrigens(unidade.getId()).stream()
                        .map(
                                c ->
                                        new UnidadeLogisticaDto.Origem(
                                                c.getEntrada().getId(),
                                                c.getEntrada().getItemChegada().getId(),
                                                c.getEntrada()
                                                        .getItemChegada()
                                                        .getItemNota()
                                                        .getId(),
                                                c.getQuantidade()))
                        .toList();
        return new UnidadeLogisticaDto.Detalhe(
                UnidadeLogisticaDto.Resumo.de(
                        unidade,
                        estoque.disponiveis(List.of(unidade.getId())).contains(unidade.getId())),
                origens);
    }

    private String hash(String acao, Long recursoId, Object dados) {
        return NfeXmlService.hash(acao + ":" + recursoId + ":" + mapper.writeValueAsString(dados));
    }

    private UnidadeLogisticaDto.Resultado repetida(Long pedidoId, UUID operacaoId, String hash) {
        var anterior = operacoes.findByPedidoIdAndOperacaoId(pedidoId, operacaoId.toString());
        if (anterior.isEmpty()) return null;
        if (!anterior.get().getConteudoHash().equals(hash))
            throw RegraNegocioException.conflito(
                    "OPERACAO_REUTILIZADA",
                    "Identificador de operação já utilizado com outro conteúdo.");
        return mapper.readValue(anterior.get().getResultado(), UnidadeLogisticaDto.Resultado.class);
    }

    private UnidadeLogisticaDto.Resultado concluir(
            PedidoEntrada p,
            PedidoEntradaDto.Resumo antes,
            UUID operacaoId,
            String hash,
            String acao,
            String motivo,
            List<UnidadeLogisticaDto.Detalhe> unidadesAntes,
            List<UnidadeLogistica> alteradas) {
        unidades.flush();
        var depois = alteradas.stream().map(this::detalhe).toList();
        var pedido =
                pedidos.registrar(
                        p,
                        antes,
                        acao,
                        CadastroSupport.motivo(motivo),
                        Map.of("operacaoId", operacaoId, "antes", unidadesAntes, "depois", depois));
        var resultado =
                new UnidadeLogisticaDto.Resultado(operacaoId, p.getId(), pedido.versao(), depois);
        operacoes.saveAndFlush(
                new OperacaoUnidade(
                        p,
                        operacaoId.toString(),
                        hash,
                        acao,
                        acesso.usuario(),
                        agora(),
                        mapper.writeValueAsString(resultado)));
        return resultado;
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
