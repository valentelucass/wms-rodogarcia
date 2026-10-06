package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.ItemPedidoSaida;
import br.com.rodogarcia.wms.models.OperacaoSaida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.ItemPedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.OperacaoSaidaRepository;
import br.com.rodogarcia.wms.repositories.PedidoSaidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
public class PedidoSaidaService {
    private final PedidoSaidaRepository pedidos;
    private final ItemPedidoSaidaRepository itens;
    private final ReservaSaidaRepository reservas;
    private final OperacaoSaidaRepository operacoes;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final UnidadeLogisticaRepository unidades;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final EstoqueService estoque;
    private final AcessoService acesso;
    private final AuditoriaService auditoria;
    private final JsonMapper mapper;
    private final Clock clock;
    private final br.com.rodogarcia.wms.repositories.ResolucaoRemanescenteRepository resolucoes;
    private final br.com.rodogarcia.wms.repositories.CargaInicialRepository cargas;
    private final br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository conteudos;
    private final OperacaoAdministrativaService administrativas;

    public PedidoSaidaService(
            PedidoSaidaRepository pedidos,
            ItemPedidoSaidaRepository itens,
            ReservaSaidaRepository reservas,
            OperacaoSaidaRepository operacoes,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            UnidadeLogisticaRepository unidades,
            OcupacaoEnderecoRepository ocupacoes,
            EstoqueService estoque,
            AcessoService acesso,
            AuditoriaService auditoria,
            JsonMapper mapper,
            Clock clock,
            br.com.rodogarcia.wms.repositories.ResolucaoRemanescenteRepository resolucoes,
            br.com.rodogarcia.wms.repositories.CargaInicialRepository cargas,
            br.com.rodogarcia.wms.repositories.ConteudoUnidadeRepository conteudos,
            OperacaoAdministrativaService administrativas) {
        this.pedidos = pedidos;
        this.itens = itens;
        this.reservas = reservas;
        this.operacoes = operacoes;
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.unidades = unidades;
        this.ocupacoes = ocupacoes;
        this.estoque = estoque;
        this.acesso = acesso;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
        this.resolucoes = resolucoes;
        this.cargas = cargas;
        this.conteudos = conteudos;
        this.administrativas = administrativas;
    }

    @Transactional
    public PedidoSaidaDto.Confirmacao criar(@NotNull @Valid PedidoSaidaDto.Criar dados) {
        autorizar(dados.clienteId(), dados.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(dados.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        String referencia = CadastroSupport.codigo(dados.referencia());
        String hash = hash("CRIACAO_SAIDA", dados);
        var anterior =
                pedidos.findByClienteIdAndArmazemIdAndReferencia(
                        dados.clienteId(), dados.armazemId(), referencia);
        if (anterior.isPresent()) {
            var repetida = repetida(anterior.get().getId(), dados.operacaoId(), hash);
            if (repetida != null) return repetida;
            throw conflito("REFERENCIA_DUPLICADA", "Referência já usada por outro pedido.");
        }
        CadastroSupport.ativo(cliente);
        CadastroSupport.ativo(armazem);
        var ids = dados.itens().stream().map(PedidoSaidaDto.ItemCriar::produtoId).toList();
        if (new HashSet<>(ids).size() != ids.size())
            throw CadastroSupport.invalido("Informe cada produto uma única vez.");
        var produtosPedido = new HashMap<Long, Produto>();
        for (Long id : ids.stream().sorted().toList()) {
            var produto =
                    produtos.buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            CadastroSupport.ativo(produto);
            if (!produto.getCliente().getId().equals(cliente.getId()))
                throw CadastroSupport.invalido("Produto pertence a outro proprietário.");
            produtosPedido.put(id, produto);
        }
        var p = pedidos.saveAndFlush(new PedidoSaida(cliente, armazem, referencia, agora()));
        for (var i : dados.itens()) {
            var produto = produtosPedido.get(i.produtoId());
            CadastroSupport.quantidade(produto, i.quantidade());
            itens.save(new ItemPedidoSaida(p, produto, i.quantidade()));
        }
        itens.flush();
        var itensPedido = itens.buscarDoPedido(p.getId());
        var disponiveis = candidatas(p, itensPedido);
        for (var i : itensPedido) {
            var saldo =
                    disponiveis.stream()
                            .filter(u -> u.getProduto().getId().equals(i.getProduto().getId()))
                            .map(UnidadeLogistica::getQuantidade)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (saldo.compareTo(i.getQuantidade()) < 0)
                throw conflito(
                        "SALDO_INSUFICIENTE", "Pedido excede o saldo disponível de um dos itens.");
        }
        return concluir(
                p,
                null,
                dados.operacaoId(),
                hash,
                "CRIACAO_SAIDA",
                dados.motivo(),
                List.of(),
                false,
                null,
                null);
    }

    @Transactional
    public PedidoSaidaDto.Confirmacao resolverRemanescente(
            @NotNull @Valid br.com.rodogarcia.wms.dto.EncerramentoDto.Remanescente d) {
        acesso.exigirGestor();
        autorizar(d.clienteId(), d.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(d.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(d.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        String hash =
                administrativas.hash(
                        "RESOLUCAO_REMANESCENTE", List.of(d.clienteId(), d.armazemId()), d);
        var replay =
                administrativas.repetida(d.operacaoId(), hash, PedidoSaidaDto.Confirmacao.class);
        if (replay != null) return replay;
        if (cliente.getSituacao() == br.com.rodogarcia.wms.models.SituacaoCadastro.INATIVO
                || armazem.getSituacao() == br.com.rodogarcia.wms.models.SituacaoCadastro.INATIVO)
            throw conflito("CADASTRO_INATIVO", "Resolução não reativa cadastro inativo.");
        var carga =
                d.cargaInicialId() == null
                        ? null
                        : cargas.findById(d.cargaInicialId())
                                .orElseThrow(RegraNegocioException::naoEncontrado);
        if (carga != null
                && (!carga.getCliente().getId().equals(d.clienteId())
                        || !carga.getArmazem().getId().equals(d.armazemId())
                        || carga.getSituacao()
                                != br.com.rodogarcia.wms.models.SituacaoCargaInicial.PREPARADA))
            throw CadastroSupport.invalido(
                    "Carga de resolução deve estar PREPARADA no mesmo contexto.");
        var porId = new HashMap<Long, UnidadeLogistica>();
        var quantidades = new java.util.TreeMap<Long, BigDecimal>();
        boolean encerrando =
                cliente.getSituacao() != br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO
                        || armazem.getSituacao()
                                != br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO;
        for (var linha :
                d.unidades().stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        br.com.rodogarcia.wms.dto.EncerramentoDto.Unidade
                                                ::unidadeId))
                        .toList()) {
            var u =
                    unidades.findById(linha.unidadeId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!u.getPedido().getCliente().getId().equals(d.clienteId())
                    || !u.getPedido().getArmazem().getId().equals(d.armazemId())
                    || porId.put(u.getId(), u) != null
                    || !u.isAtiva()
                    || u.getVersao() != linha.versao()
                    || u.getQuantidade().compareTo(linha.quantidade()) != 0)
                throw conflito(
                        "REMANESCENTE_DIVERGENTE",
                        "Informe unidades existentes completas e versões atuais do contexto.");
            var produto =
                    produtos.buscarParaAtualizar(u.getProduto().getId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (produto.getSituacao() == br.com.rodogarcia.wms.models.SituacaoCadastro.INATIVO)
                throw conflito("CADASTRO_INATIVO", "Produto inativo não admite resolução física.");
            encerrando |=
                    produto.getSituacao() != br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO;
            encerrando |=
                    ocupacoes.buscarDasUnidades(List.of(u.getId())).stream()
                            .anyMatch(
                                    o ->
                                            o.getEndereco().getSituacao()
                                                    != br.com.rodogarcia.wms.models.SituacaoCadastro
                                                            .ATIVO);
            quantidades.merge(produto.getId(), linha.quantidade(), BigDecimal::add);
            if (carga != null
                    && (conteudos.buscarOrigens(u.getId()).stream()
                                    .anyMatch(
                                            c ->
                                                    !c.getEntrada()
                                                            .getId()
                                                            .equals(carga.getEntrada().getId()))
                            || !u.getProduto().getId().equals(carga.getProduto().getId())))
                throw conflito(
                        "CARGA_DIVERGENTE",
                        "Unidades precisam pertencer exclusivamente à entrada preparada identificada.");
        }
        if (!encerrando && carga == null)
            throw CadastroSupport.invalido(
                    "Caminho exige cadastro em encerramento ou carga preparada identificada.");
        if (carga != null
                && (new HashSet<>(d.etiquetas()).size() != d.etiquetas().size()
                        || !new HashSet<>(d.etiquetas())
                                .equals(
                                        porId.values().stream()
                                                .map(UnidadeLogistica::getCodigo)
                                                .collect(Collectors.toSet()))
                        || quantidades.values().stream()
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .compareTo(carga.getQuantidade())
                                != 0))
            throw conflito(
                    "LEITURA_INCOMPLETA",
                    "Leia e resolva todas as etiquetas da carga preparada, sem omissão.");
        var candidatas =
                unidades.candidatasResolucao(
                        d.clienteId(),
                        d.armazemId(),
                        List.copyOf(quantidades.keySet()),
                        d.cargaInicialId(),
                        List.of(
                                br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO,
                                br.com.rodogarcia.wms.models.SituacaoCadastro
                                        .ENCERRAMENTO_PENDENTE));
        if (!candidatas.stream()
                .map(UnidadeLogistica::getId)
                .collect(Collectors.toSet())
                .containsAll(porId.keySet()))
            throw conflito(
                    "UNIDADE_INDISPONIVEL",
                    "Resolução preserva bloqueios, avaria, contagem, capacidade e reserva.");
        String referencia = CadastroSupport.codigo(d.referencia());
        if (pedidos.findByClienteIdAndArmazemIdAndReferencia(
                        d.clienteId(), d.armazemId(), referencia)
                .isPresent())
            throw conflito("REFERENCIA_DUPLICADA", "Pedido de resolução já identificado.");
        var p = pedidos.saveAndFlush(new PedidoSaida(cliente, armazem, referencia, agora()));
        quantidades.forEach(
                (id, q) ->
                        itens.save(new ItemPedidoSaida(p, produtos.findById(id).orElseThrow(), q)));
        itens.flush();
        var itensPedido = itens.buscarDoPedido(p.getId());
        var selecionadas =
                d.unidades().stream()
                        .map(u -> new PedidoSaidaDto.Selecao(u.unidadeId(), u.quantidade()))
                        .toList();
        validarSelecao(itensPedido, candidatas, selecionadas);
        boolean excecao = !segueFifo(selecionadas, itensPedido, candidatas);
        if (excecao && (d.justificativaFifo() == null || d.justificativaFifo().isBlank()))
            throw conflito(
                    "JUSTIFICATIVA_FIFO_NECESSARIA",
                    "Gestor deve justificar a exceção da seleção integral ao FIFO.");
        resolucoes.saveAndFlush(
                new br.com.rodogarcia.wms.models.ResolucaoRemanescente(
                        p,
                        carga,
                        mapper.writeValueAsString(d),
                        CadastroSupport.motivo(d.motivo()),
                        acesso.usuario(),
                        agora()));
        var porProduto =
                itensPedido.stream().collect(Collectors.toMap(i -> i.getProduto().getId(), i -> i));
        for (var s : selecionadas) {
            var u = porId.get(s.unidadeId());
            u.reservarSaida(p, agora());
            reservas.save(
                    new ReservaSaida(
                            porProduto.get(u.getProduto().getId()),
                            u,
                            d.operacaoId().toString(),
                            s.quantidade(),
                            agora()));
        }
        p.atualizar(SituacaoPedidoSaida.RESERVADO, agora());
        unidades.flush();
        reservas.flush();
        var resposta =
                concluir(
                        p,
                        null,
                        d.operacaoId(),
                        hash,
                        "RESERVA_SAIDA",
                        d.motivo(),
                        selecionadas,
                        excecao,
                        excecao ? acesso.usuario() : null,
                        d.justificativaFifo());
        auditoria.registrar(
                "PEDIDO_SAIDA",
                p.getId(),
                "RESOLUCAO_REMANESCENTE",
                CadastroSupport.motivo(d.motivo()),
                null,
                Map.of("resolucao", d, "pedido", resposta));
        administrativas.salvar(
                d.operacaoId(),
                "RESOLUCAO_REMANESCENTE",
                cliente,
                armazem,
                p.getId(),
                hash,
                resposta);
        return resposta;
    }

    public PedidoSaidaDto.Detalhe consultar(@NotNull @Positive Long id) {
        var p = obter(id);
        return detalhe(p);
    }

    public PaginaResponse<PedidoSaidaDto.Detalhe> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        autorizar(clienteId, armazemId);
        return PaginaResponse.de(
                pedidos.findByClienteIdAndArmazemId(
                        clienteId, armazemId, CadastroSupport.pagina(pagina, tamanho)),
                this::detalhe);
    }

    // A sugestão é informativa. O comando recalcula após adquirir os locks compartilhados.
    public PedidoSaidaDto.Sugestao sugerir(@NotNull @Positive Long id) {
        var p = obter(id);
        exigir(p, SituacaoPedidoSaida.RASCUNHO);
        var itensPedido = itens.buscarDoPedido(id);
        return new PedidoSaidaDto.Sugestao(
                p.getVersao(), fifo(itensPedido, candidatas(p, itensPedido)));
    }

    @Transactional
    public PedidoSaidaDto.Confirmacao justificar(
            @NotNull @Positive Long id, @NotNull @Valid PedidoSaidaDto.Justificar dados) {
        var p = bloquear(id);
        String hash = hash("JUSTIFICATIVA_FIFO", dados);
        var repetida = repetida(id, dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        versao(p, dados.versao());
        exigir(p, SituacaoPedidoSaida.RASCUNHO);
        var itensPedido = validarVinculos(p);
        var candidatas = candidatas(p, itensPedido);
        validarSelecao(itensPedido, candidatas, dados.selecoes());
        boolean excecao = !segueFifo(dados.selecoes(), itensPedido, candidatas);
        var antes = detalhe(p);
        p.atualizar(p.getSituacao(), agora());
        return concluir(
                p,
                antes,
                dados.operacaoId(),
                hash,
                "JUSTIFICATIVA_FIFO",
                dados.motivo(),
                dados.selecoes(),
                excecao,
                acesso.usuario(),
                CadastroSupport.motivo(dados.motivo()));
    }

    @Transactional
    public PedidoSaidaDto.Confirmacao reservar(
            @NotNull @Positive Long id, @NotNull @Valid PedidoSaidaDto.Reservar dados) {
        if (dados.justificativaId() != null) acesso.exigirSupervisor();
        var p = bloquear(id);
        String hash = hash("RESERVA_SAIDA", dados);
        var repetida = repetida(id, dados.operacaoId(), hash);
        if (repetida != null) {
            if (repetida.excecaoFifo()) acesso.exigirSupervisor();
            return repetida;
        }
        versao(p, dados.versao());
        exigir(p, SituacaoPedidoSaida.RASCUNHO);
        var itensPedido = validarVinculos(p);
        var candidatas = candidatas(p, itensPedido);
        List<PedidoSaidaDto.Selecao> selecao;
        String justificadaPor = null;
        String justificativa = null;
        if (dados.justificativaId() != null) {
            var registro =
                    operacoes
                            .findByPedidoIdAndOperacaoId(id, dados.justificativaId().toString())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!registro.getTipo().equals("JUSTIFICATIVA_FIFO"))
                throw CadastroSupport.invalido("A operação indicada não é uma justificativa FIFO.");
            var proposta =
                    mapper.readValue(registro.getResultado(), PedidoSaidaDto.Confirmacao.class);
            if (proposta.pedido().versao() != p.getVersao())
                throw conflito(
                        "JUSTIFICATIVA_DESATUALIZADA",
                        "Pedido alterado; registre novamente a seleção justificada.");
            selecao = proposta.selecoes();
            justificadaPor = proposta.justificadaPor();
            justificativa = proposta.justificativa();
        } else {
            selecao = fifo(itensPedido, candidatas);
        }
        validarSelecao(itensPedido, candidatas, selecao);
        boolean excecao = !segueFifo(selecao, itensPedido, candidatas);
        if (excecao) acesso.exigirSupervisor();
        var antes = detalhe(p);
        var porId = candidatas.stream().collect(Collectors.toMap(UnidadeLogistica::getId, u -> u));
        var porProduto =
                itensPedido.stream().collect(Collectors.toMap(i -> i.getProduto().getId(), i -> i));
        for (var s : selecao) {
            var u = porId.get(s.unidadeId());
            u.reservarSaida(p, agora());
            reservas.save(
                    new ReservaSaida(
                            porProduto.get(u.getProduto().getId()),
                            u,
                            dados.operacaoId().toString(),
                            s.quantidade(),
                            agora()));
        }
        p.atualizar(SituacaoPedidoSaida.RESERVADO, agora());
        unidades.flush();
        reservas.flush();
        return concluir(
                p,
                antes,
                dados.operacaoId(),
                hash,
                "RESERVA_SAIDA",
                dados.motivo(),
                selecao,
                excecao,
                justificadaPor,
                justificativa);
    }

    @Transactional
    public PedidoSaidaDto.Confirmacao cancelar(
            @NotNull @Positive Long id, @NotNull @Valid PedidoSaidaDto.Comando dados) {
        acesso.exigirSupervisor();
        return encerrar(id, dados, true);
    }

    @Transactional
    public PedidoSaidaDto.Confirmacao reverter(
            @NotNull @Positive Long id, @NotNull @Valid PedidoSaidaDto.Comando dados) {
        acesso.exigirSupervisor();
        return encerrar(id, dados, false);
    }

    // Fronteira preparada para BE10: não confirma retirada nem altera quantidade física.
    @Transactional
    public PedidoSaidaDto.Detalhe revalidar(@NotNull @Positive Long id) {
        var p = bloquear(id);
        exigir(p, SituacaoPedidoSaida.RESERVADO);
        var detalhe = detalhe(p);
        if (!detalhe.podeProsseguir())
            throw conflito(
                    "RESERVA_IMPEDIDA",
                    "Reserva com impedimento; preserve-a e resolva a ocorrência antes de prosseguir.");
        return detalhe;
    }

    private PedidoSaidaDto.Confirmacao encerrar(
            Long id, PedidoSaidaDto.Comando dados, boolean cancelar) {
        var p = bloquear(id);
        String acao = cancelar ? "CANCELAMENTO_SAIDA" : "REVERSAO_RESERVA";
        String hash = hash(acao, dados);
        var repetida = repetida(id, dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        versao(p, dados.versao());
        if (cancelar) exigir(p, SituacaoPedidoSaida.RASCUNHO, SituacaoPedidoSaida.RESERVADO);
        else exigir(p, SituacaoPedidoSaida.RESERVADO);
        var antes = detalhe(p);
        for (var r : reservas.buscarDoPedido(id)) {
            if (r.getSituacao() != SituacaoReservaSaida.ATIVA) continue;
            var u = r.getUnidade();
            if (u.getReservaSaida() == null || !u.getReservaSaida().getId().equals(id))
                throw conflito(
                        "RESERVA_INCONSISTENTE", "Reserva e unidade precisam ser conciliadas.");
            u.reservarSaida(null, agora());
            r.encerrar(
                    cancelar ? SituacaoReservaSaida.CANCELADA : SituacaoReservaSaida.REVERTIDA,
                    agora());
        }
        p.atualizar(
                cancelar ? SituacaoPedidoSaida.CANCELADO : SituacaoPedidoSaida.RASCUNHO, agora());
        unidades.flush();
        reservas.flush();
        return concluir(
                p,
                antes,
                dados.operacaoId(),
                hash,
                acao,
                dados.motivo(),
                List.of(),
                false,
                null,
                null);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public PedidoSaida bloquear(Long id) {
        // Projeção escalar evita entidades em cache antes dos locks. Nunca bloquear entrada aqui.
        var escopo = pedidos.buscarEscopo(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(escopo.getClienteId(), escopo.getArmazemId());
        clientes.buscarParaAtualizar(escopo.getClienteId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        armazens.buscarParaAtualizar(escopo.getArmazemId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        if (resolucoes.findByPedidoSaidaId(id).isPresent()) acesso.exigirGestor();
        return pedidos.buscarParaAtualizar(id).orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private PedidoSaida obter(Long id) {
        var p = pedidos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(p.getCliente().getId(), p.getArmazem().getId());
        return p;
    }

    private void autorizar(Long clienteId, Long armazemId) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
    }

    private List<ItemPedidoSaida> validarVinculos(PedidoSaida p) {
        CadastroSupport.ativo(p.getCliente());
        CadastroSupport.ativo(p.getArmazem());
        var itensPedido = itens.buscarDoPedido(p.getId());
        for (var i : itensPedido) {
            CadastroSupport.ativo(
                    produtos.buscarParaAtualizar(i.getProduto().getId())
                            .orElseThrow(RegraNegocioException::naoEncontrado));
            CadastroSupport.quantidade(i.getProduto(), i.getQuantidade());
        }
        return itensPedido;
    }

    private List<UnidadeLogistica> candidatas(PedidoSaida p, List<ItemPedidoSaida> itensPedido) {
        return estoque.candidatas(
                p.getCliente().getId(),
                p.getArmazem().getId(),
                itensPedido.stream().map(i -> i.getProduto().getId()).toList());
    }

    private List<PedidoSaidaDto.Selecao> fifo(
            List<ItemPedidoSaida> itensPedido, List<UnidadeLogistica> candidatas) {
        var ids = candidatas.stream().map(UnidadeLogistica::getId).toList();
        var porUnidade = new HashMap<Long, Endereco>();
        Comparator<Endereco> endereco =
                Comparator.comparingInt(Endereco::getNivel)
                        .thenComparingInt(Endereco::getSequenciaColeta)
                        .thenComparing(Endereco::getCodigo)
                        .thenComparing(Endereco::getId);
        if (!ids.isEmpty())
            ocupacoes
                    .buscarDasUnidades(ids)
                    .forEach(
                            o ->
                                    porUnidade.merge(
                                            o.getUnidade().getId(),
                                            o.getEndereco(),
                                            (a, b) -> endereco.compare(a, b) <= 0 ? a : b));
        Comparator<UnidadeLogistica> ordem =
                Comparator.comparing(UnidadeLogistica::getDataFifo)
                        .thenComparing(u -> porUnidade.get(u.getId()), endereco)
                        .thenComparing(UnidadeLogistica::getId);
        var resultado = new ArrayList<PedidoSaidaDto.Selecao>();
        for (var i : itensPedido) {
            BigDecimal restante = i.getQuantidade();
            for (var u :
                    candidatas.stream()
                            .filter(u -> u.getProduto().getId().equals(i.getProduto().getId()))
                            .sorted(ordem)
                            .toList()) {
                if (restante.signum() == 0) break;
                // Bobina não é fracionada pelo fluxo autorizado de parcial de pallet.
                if (u.getTipo() == TipoUnidadeLogistica.BOBINA
                        && u.getQuantidade().compareTo(restante) > 0) continue;
                BigDecimal quantidade = u.getQuantidade().min(restante);
                resultado.add(new PedidoSaidaDto.Selecao(u.getId(), quantidade));
                restante = restante.subtract(quantidade);
            }
            if (restante.signum() > 0)
                throw conflito(
                        "SALDO_INSUFICIENTE",
                        "Saldo elegível insuficiente para reservar o pedido integral.");
        }
        return List.copyOf(resultado);
    }

    private void validarSelecao(
            List<ItemPedidoSaida> itensPedido,
            List<UnidadeLogistica> candidatas,
            List<PedidoSaidaDto.Selecao> selecao) {
        var porId = candidatas.stream().collect(Collectors.toMap(UnidadeLogistica::getId, u -> u));
        var totais = new HashMap<Long, BigDecimal>();
        var vistos = new HashSet<Long>();
        for (var s : selecao) {
            if (!vistos.add(s.unidadeId()))
                throw CadastroSupport.invalido("Unidade repetida na seleção.");
            var u = porId.get(s.unidadeId());
            if (u == null)
                throw conflito(
                        "UNIDADE_INDISPONIVEL",
                        "Unidade fora do alcance, produto ou disponibilidade deste pedido.");
            CadastroSupport.quantidade(u.getProduto(), s.quantidade());
            if (s.quantidade().compareTo(u.getQuantidade()) > 0)
                throw conflito("SALDO_INSUFICIENTE", "Quantidade excede o conteúdo da unidade.");
            if (u.getTipo() == TipoUnidadeLogistica.BOBINA
                    && s.quantidade().compareTo(u.getQuantidade()) != 0)
                throw CadastroSupport.invalido("Reserva parcial neste recorte exige pallet.");
            totais.merge(u.getProduto().getId(), s.quantidade(), BigDecimal::add);
        }
        for (var i : itensPedido)
            if (i.getQuantidade()
                            .compareTo(totais.getOrDefault(i.getProduto().getId(), BigDecimal.ZERO))
                    != 0)
                throw conflito(
                        "PEDIDO_NAO_INTEGRAL",
                        "A seleção precisa atender integralmente cada item do pedido.");
    }

    private boolean segueFifo(
            List<PedidoSaidaDto.Selecao> selecao,
            List<ItemPedidoSaida> itensPedido,
            List<UnidadeLogistica> candidatas) {
        try {
            return mesmaSelecao(selecao, fifo(itensPedido, candidatas));
        } catch (RegraNegocioException e) {
            if (!e.getCodigo().equals("SALDO_INSUFICIENTE")) throw e;
            // FIFO guloso não fecha bobinas indivisíveis: a seleção integral validada pode
            // ser excepcionalmente autorizada. Nunca confirmar o prefixo parcial do FIFO.
            return false;
        }
    }

    private boolean mesmaSelecao(List<PedidoSaidaDto.Selecao> a, List<PedidoSaidaDto.Selecao> b) {
        var mapa =
                b.stream()
                        .collect(
                                Collectors.toMap(
                                        PedidoSaidaDto.Selecao::unidadeId,
                                        PedidoSaidaDto.Selecao::quantidade));
        return a.size() == b.size()
                && a.stream()
                        .allMatch(
                                s ->
                                        mapa.containsKey(s.unidadeId())
                                                && mapa.get(s.unidadeId()).compareTo(s.quantidade())
                                                        == 0);
    }

    @Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.MANDATORY,
            readOnly = true)
    public PedidoSaidaDto.Detalhe detalhe(PedidoSaida p) {
        autorizar(p.getCliente().getId(), p.getArmazem().getId());
        var itensPedido = itens.buscarDoPedido(p.getId());
        var linhas = reservas.buscarDoPedido(p.getId());
        var ativas =
                linhas.stream().filter(r -> r.getSituacao() == SituacaoReservaSaida.ATIVA).toList();
        var elegiveis =
                new HashSet<>(
                        estoque.elegiveis(
                                ativas.stream().map(r -> r.getUnidade().getId()).toList()));
        elegiveis.addAll(
                estoque.elegiveisSaida(
                        ativas.stream().map(r -> r.getUnidade().getId()).toList(),
                        p.getId(),
                        br.com.rodogarcia.wms.models.TipoEndereco.SEPARACAO,
                        false));
        var impedidas =
                ativas.stream()
                        .filter(
                                r ->
                                        !elegiveis.contains(r.getUnidade().getId())
                                                || r.getUnidade().getReservaSaida() == null
                                                || !r.getUnidade()
                                                        .getReservaSaida()
                                                        .getId()
                                                        .equals(p.getId())
                                                || r.getQuantidade()
                                                                .compareTo(
                                                                        r.getUnidade()
                                                                                .getQuantidade())
                                                        > 0)
                        .map(r -> r.getUnidade().getId())
                        .distinct()
                        .sorted()
                        .toList();
        boolean integral =
                itensPedido.stream()
                        .allMatch(
                                i ->
                                        i.getQuantidade()
                                                        .compareTo(
                                                                ativas.stream()
                                                                        .filter(
                                                                                r ->
                                                                                        r.getItem()
                                                                                                .getId()
                                                                                                .equals(
                                                                                                        i
                                                                                                                .getId()))
                                                                        .map(
                                                                                ReservaSaida
                                                                                        ::getQuantidade)
                                                                        .reduce(
                                                                                BigDecimal.ZERO,
                                                                                BigDecimal::add))
                                                == 0);
        return new PedidoSaidaDto.Detalhe(
                p.getId(),
                p.getVersao(),
                p.getCliente().getId(),
                p.getArmazem().getId(),
                p.getReferencia(),
                p.getSituacao(),
                p.getCriadoEm(),
                p.getAlteradoEm(),
                itensPedido.stream()
                        .map(
                                i ->
                                        new PedidoSaidaDto.Item(
                                                i.getId(),
                                                i.getProduto().getId(),
                                                i.getProduto().getSku(),
                                                i.getProduto().getUnidadeMedida(),
                                                i.getQuantidade().setScale(6)))
                        .toList(),
                linhas.stream()
                        .map(
                                r ->
                                        new PedidoSaidaDto.Reserva(
                                                r.getId(),
                                                r.getItem().getId(),
                                                r.getUnidade().getId(),
                                                r.getUnidade().getCodigo(),
                                                r.getUnidade().getNota().getId(),
                                                r.getUnidade().getLote(),
                                                r.getUnidade().getDataFifo(),
                                                r.getQuantidade().setScale(6),
                                                r.getSituacao(),
                                                r.getCriadaEm(),
                                                r.getEncerradaEm()))
                        .toList(),
                impedidas,
                List.of(
                                        SituacaoPedidoSaida.RESERVADO,
                                        SituacaoPedidoSaida.EM_SEPARACAO,
                                        SituacaoPedidoSaida.SEPARADO)
                                .contains(p.getSituacao())
                        && !ativas.isEmpty()
                        && integral
                        && impedidas.isEmpty());
    }

    private PedidoSaidaDto.Confirmacao concluir(
            PedidoSaida p,
            PedidoSaidaDto.Detalhe antes,
            UUID operacaoId,
            String hash,
            String acao,
            String motivo,
            List<PedidoSaidaDto.Selecao> selecoes,
            boolean excecao,
            String justificadaPor,
            String justificativa) {
        pedidos.flush();
        var resultado =
                new PedidoSaidaDto.Confirmacao(
                        operacaoId, detalhe(p), selecoes, excecao, justificadaPor, justificativa);
        auditoria.registrar(
                "PEDIDO_SAIDA", p.getId(), acao, CadastroSupport.motivo(motivo), antes, resultado);
        operacoes.saveAndFlush(
                new OperacaoSaida(
                        p,
                        operacaoId.toString(),
                        hash,
                        acao,
                        acesso.usuario(),
                        CadastroSupport.motivo(motivo),
                        agora(),
                        mapper.writeValueAsString(resultado)));
        return resultado;
    }

    private PedidoSaidaDto.Confirmacao repetida(Long pedidoId, UUID operacaoId, String hash) {
        var anterior = operacoes.findByPedidoIdAndOperacaoId(pedidoId, operacaoId.toString());
        if (anterior.isEmpty()) return null;
        if (!anterior.get().getConteudoHash().equals(hash))
            throw conflito("OPERACAO_REUTILIZADA", "Identificador já usado com outro conteúdo.");
        return mapper.readValue(anterior.get().getResultado(), PedidoSaidaDto.Confirmacao.class);
    }

    private String hash(String acao, Object dados) {
        return NfeXmlService.hash(acao + ":" + mapper.writeValueAsString(dados));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static void versao(PedidoSaida p, long versao) {
        if (p.getVersao() != versao)
            throw conflito(
                    "VERSAO_DESATUALIZADA",
                    "Pedido alterado; consulte novamente antes de confirmar.");
    }

    private static void exigir(PedidoSaida p, SituacaoPedidoSaida... permitidas) {
        if (!List.of(permitidas).contains(p.getSituacao()))
            throw conflito("SITUACAO_INVALIDA", "A situação do pedido não permite esta operação.");
    }

    private static RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }
}
