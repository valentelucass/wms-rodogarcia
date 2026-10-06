package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.CargaInicialDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.CargaInicial;
import br.com.rodogarcia.wms.models.ConteudoUnidade;
import br.com.rodogarcia.wms.models.EntradaConferida;
import br.com.rodogarcia.wms.models.ResolucaoRemanescente;
import br.com.rodogarcia.wms.models.RevisaoCargaInicial;
import br.com.rodogarcia.wms.models.RevisaoContagem;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.SituacaoCargaInicial;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoSaida;
import br.com.rodogarcia.wms.models.SituacaoRevisaoContagem;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.CargaInicialRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.RevisaoCargaInicialRepository;
import jakarta.persistence.EntityManager;
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
public class CargaInicialService {
    private final CargaInicialRepository cargas;
    private final RevisaoCargaInicialRepository revisoes;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final ProdutoRepository produtos;
    private final EntradaConferidaRepository entradas;
    private final PedidoEntradaService pedidos;
    private final NotaEntradaService notas;
    private final RecebimentoService recebimento;
    private final UnidadeLogisticaService unitizacao;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final EntityManager em;
    private final Clock clock;

    public CargaInicialService(
            CargaInicialRepository cargas,
            RevisaoCargaInicialRepository revisoes,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            ProdutoRepository produtos,
            EntradaConferidaRepository entradas,
            PedidoEntradaService pedidos,
            NotaEntradaService notas,
            RecebimentoService recebimento,
            UnidadeLogisticaService unitizacao,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            AcessoService acesso,
            JsonMapper mapper,
            EntityManager em,
            Clock clock) {
        this.cargas = cargas;
        this.revisoes = revisoes;
        this.clientes = clientes;
        this.armazens = armazens;
        this.produtos = produtos;
        this.entradas = entradas;
        this.pedidos = pedidos;
        this.notas = notas;
        this.recebimento = recebimento;
        this.unitizacao = unitizacao;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.acesso = acesso;
        this.mapper = mapper;
        this.em = em;
        this.clock = clock;
    }

    @Transactional
    public CargaInicialDto.Resultado criar(@NotNull @Valid CargaInicialDto.Criar dados) {
        acesso.exigirSupervisor();
        autorizar(dados.clienteId(), dados.armazemId());
        var cliente =
                clientes.buscarParaAtualizar(dados.clienteId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.buscarParaAtualizar(dados.armazemId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        String hash =
                operacoes.hash(
                        "REGISTRO_CARGA", List.of(dados.clienteId(), dados.armazemId()), dados);
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, CargaInicialDto.Resultado.class);
        if (repetida != null) return repetida;
        CadastroSupport.ativo(cliente);
        CadastroSupport.ativo(armazem);
        var produto =
                produtos.buscarParaAtualizar(dados.produtoId())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(produto);
        if (!produto.getCliente().getId().equals(cliente.getId()))
            throw RegraNegocioException.naoEncontrado();
        CadastroSupport.quantidade(produto, dados.quantidade());
        String referencia = CadastroSupport.codigo(dados.referencia());
        if (cargas.findByClienteIdAndArmazemIdAndReferencia(
                        cliente.getId(), armazem.getId(), referencia)
                .isPresent())
            throw conflito("REFERENCIA_DUPLICADA", "Referência do estágio já registrada.");
        var c =
                cargas.saveAndFlush(
                        new CargaInicial(
                                cliente,
                                armazem,
                                produto,
                                referencia,
                                dados.etiquetaFornecida() == null
                                        ? null
                                        : CadastroSupport.texto(dados.etiquetaFornecida()),
                                dados.quantidade(),
                                agora()));
        revisar(c, dados.dados(), dados.motivo());
        var resultado = resultado(c);
        salvar(c, "REGISTRO_CARGA", dados.operacaoId(), hash, dados.motivo(), null, resultado);
        return resultado;
    }

    @Transactional
    public CargaInicialDto.Resultado revisar(
            @NotNull @Positive Long id, @NotNull @Valid CargaInicialDto.Revisar dados) {
        acesso.exigirSupervisor();
        var c = bloquear(id);
        String hash = operacoes.hash("REVISAO_CARGA", id, dados);
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, CargaInicialDto.Resultado.class);
        if (repetida != null) return repetida;
        conferir(c, dados.versao());
        var antes = resultado(c);
        revisar(c, dados.dados(), dados.motivo());
        var resposta = resultado(c);
        salvar(c, "REVISAO_CARGA", dados.operacaoId(), hash, dados.motivo(), antes, resposta);
        return resposta;
    }

    @Transactional
    public CargaInicialDto.Resultado preparar(
            @NotNull @Positive Long id, @NotNull @Valid CargaInicialDto.Confirmar dados) {
        acesso.exigirSupervisor();
        var c = bloquear(id);
        String hash = operacoes.hash("PREPARACAO_CARGA", id, dados);
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, CargaInicialDto.Resultado.class);
        if (repetida != null) return repetida;
        conferir(c, dados.versao());
        var revisao = atual(c);
        if (c.getRevisaoAtual() != dados.revisao()
                || !revisao.getConteudoHash().equals(dados.conteudoHash()))
            throw conflito("REVISAO_DESATUALIZADA", "Confirme a revisão e hash atuais do estágio.");
        String leitura =
                c.getEtiquetaFornecida() == null ? c.getReferencia() : c.getEtiquetaFornecida();
        if (!leitura.equals(dados.leitura()))
            throw conflito(
                    "LEITURA_DIVERGENTE",
                    "Leitura não corresponde à referência/etiqueta fornecida.");
        var proposta = mapper.readValue(revisao.getDadosJson(), CargaInicialDto.Dados.class);
        if (proposta.fonte() == null || proposta.fonte().isBlank())
            throw conflito("ORIGEM_PENDENTE", "Informe a fonte comprovada da regularização.");
        var antes = resultado(c);
        EntradaConferida entrada;
        if (proposta.entradaExistenteId() != null) {
            entrada =
                    entradas.findById(proposta.entradaExistenteId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            conferirEntrada(c, entrada);
            if (cargas.existsByEntradaId(entrada.getId()))
                throw conflito("ENTRADA_JA_VINCULADA", "A entrada já regulariza outro estágio.");
        } else entrada = efetivar(c, proposta, dados.operacaoId(), dados.motivo());
        conferirEntrada(c, entrada);
        if (entrada.getUnitizadaEm() == null) {
            if (proposta.unidades() == null || proposta.unidades().isEmpty())
                throw conflito(
                        "UNITIZACAO_PENDENTE",
                        "Informe embalagem/condição/quantidade comprovadas de todas as unidades.");
            var p = entrada.getItemChegada().getChegada().getPedido();
            unitizacao.unitizar(
                    p.getId(),
                    entrada.getId(),
                    new UnidadeLogisticaDto.Unitizar(
                            chave(dados.operacaoId(), "UNITIZAR"),
                            p.getVersao(),
                            dados.motivo(),
                            proposta.unidades()));
        }
        c.preparar(entrada, agora());
        cargas.flush();
        var resposta = resultado(c);
        salvar(c, "PREPARACAO_CARGA", dados.operacaoId(), hash, dados.motivo(), antes, resposta);
        return resposta;
    }

    @Transactional
    public CargaInicialDto.Resultado confirmar(
            @NotNull @Positive Long id, @NotNull @Valid CargaInicialDto.Confirmar dados) {
        acesso.exigirSupervisor();
        var c = bloquear(id);
        String hash = operacoes.hash("CONFIRMACAO_CARGA", id, dados);
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, CargaInicialDto.Resultado.class);
        if (repetida != null) return repetida;
        var r = atual(c);
        if (c.getVersao() != dados.versao()
                || c.getSituacao() != SituacaoCargaInicial.PREPARADA
                || r.getNumero() != dados.revisao()
                || !r.getConteudoHash().equals(dados.conteudoHash()))
            throw conflito("ESTAGIO_DESATUALIZADO", "Confirme a preparação e revisão atuais.");
        String leitura =
                c.getEtiquetaFornecida() == null ? c.getReferencia() : c.getEtiquetaFornecida();
        var etiquetas = etiquetas(c.getEntrada().getId());
        if (!leitura.equals(dados.leitura())
                || etiquetas.isEmpty()
                || new HashSet<>(dados.etiquetasUnidades()).size()
                        != dados.etiquetasUnidades().size()
                || !new HashSet<>(etiquetas).equals(new HashSet<>(dados.etiquetasUnidades())))
            throw conflito(
                    "LEITURA_INCOMPLETA",
                    "Leia a referência e todas as etiquetas efetivas, sem repetição ou omissão.");
        var quantidade =
                conteudosDaEntrada(c.getEntrada().getId()).stream()
                        .map(ConteudoUnidade::getQuantidade)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (quantidade.compareTo(c.getQuantidade()) != 0)
            throw conflito(
                    "SALDO_ESTAGIO_DIVERGENTE",
                    "Conteúdo mudou após preparação; resolva a divergência antes da conferência.");
        var antes = resultado(c);
        c.confirmar(agora());
        cargas.flush();
        var resposta = resultado(c);
        salvar(c, "CONFIRMACAO_CARGA", dados.operacaoId(), hash, dados.motivo(), antes, resposta);
        return resposta;
    }

    @Transactional
    public CargaInicialDto.Resultado cancelar(
            @NotNull @Positive Long id, @NotNull @Valid CargaInicialDto.Cancelar dados) {
        acesso.exigirGestor();
        var c = bloquear(id);
        String hash = operacoes.hash("CANCELAMENTO_CARGA", id, dados);
        var repetida =
                operacoes.repetida(dados.operacaoId(), hash, CargaInicialDto.Resultado.class);
        if (repetida != null) return repetida;
        conferir(c, dados.versao());
        var antes = resultado(c);
        c.cancelar(agora());
        cargas.flush();
        var resposta = resultado(c);
        salvar(c, "CANCELAMENTO_CARGA", dados.operacaoId(), hash, dados.motivo(), antes, resposta);
        return resposta;
    }

    @Transactional
    public CargaInicialDto.Resultado resolverCancelamento(
            @NotNull @Positive Long id, @NotNull @Valid CargaInicialDto.ResolverCancelamento d) {
        acesso.exigirGestor();
        var c = bloquear(id);
        String hash = operacoes.hash("CANCELAMENTO_CARGA", List.of("RESOLUCAO_PREPARADA", id), d);
        var replay = operacoes.repetida(d.operacaoId(), hash, CargaInicialDto.Resultado.class);
        if (replay != null) return replay;
        if (c.getVersao() != d.versao()
                || c.getSituacao() != SituacaoCargaInicial.PREPARADA
                || c.getEntrada() == null
                || c.getEntrada().getUnitizadaEm() == null)
            throw conflito(
                    "ESTAGIO_DESATUALIZADO", "Resolução exige estágio preparado e versão atual.");
        var composicao = conteudosDaEntrada(c.getEntrada().getId());
        var ids =
                composicao.stream()
                        .map(co -> co.getUnidade().getId())
                        .collect(java.util.stream.Collectors.toSet());
        for (var co : composicao) {
            var u = co.getUnidade();
            if (co.getQuantidade().signum() > 0
                    || u.isAtiva()
                    || u.getReservaSaida() != null
                    || !em.createQuery(
                                    "select o.id from OcupacaoEndereco o where o.unidade.id=:id",
                                    Long.class)
                            .setParameter("id", u.getId())
                            .getResultList()
                            .isEmpty())
                throw conflito(
                        "FISICO_ESTAGIO_PENDENTE",
                        "Resolva todo o físico/reserva/ocupação antes de cancelar a preparação.");
        }
        if (composicao.isEmpty() || (d.pedidoResolucaoId() == null) == d.contagensIds().isEmpty())
            throw CadastroSupport.invalido(
                    "Informe exatamente um pedido integral ou a lista exaustiva de contagens aplicadas.");
        if (d.pedidoResolucaoId() != null) {
            var rr =
                    em.createQuery(
                                    "select r from ResolucaoRemanescente r where r.pedidoSaida.id=:id and r.carga.id=:carga",
                                    ResolucaoRemanescente.class)
                            .setParameter("id", d.pedidoResolucaoId())
                            .setParameter("carga", id)
                            .getResultList();
            if (rr.size() != 1
                    || rr.getFirst().getPedidoSaida().getSituacao() != SituacaoPedidoSaida.RETIRADO)
                throw conflito(
                        "RESOLUCAO_ESTAGIO_INCOMPLETA",
                        "Pedido identificado da carga precisa estar integralmente RETIRADO.");
        } else {
            if (new HashSet<>(d.contagensIds()).size() != d.contagensIds().size())
                throw CadastroSupport.invalido("Não repita contagens de resolução.");
            var leituras =
                    em.createQuery(
                                    "select r from RevisaoContagem r where r.contagem.id in :ids and r.numero=r.contagem.revisaoAtual",
                                    RevisaoContagem.class)
                            .setParameter("ids", d.contagensIds())
                            .getResultList();
            if (leituras.size() != d.contagensIds().size()
                    || !leituras.stream()
                            .map(r -> r.getContagem().getUnidade().getId())
                            .collect(java.util.stream.Collectors.toSet())
                            .equals(ids)
                    || leituras.stream()
                            .anyMatch(
                                    r ->
                                            r.getSituacao() != SituacaoRevisaoContagem.APLICADA
                                                    || r.getContado().signum() != 0
                                                    || r.getEfeitoJson() == null))
                throw conflito(
                        "RESOLUCAO_ESTAGIO_INCOMPLETA",
                        "Cada unidade exige contagem atual APLICADA a zero, com efeito identificado.");
        }
        var antes = resultado(c);
        c.cancelarPreparada(agora());
        cargas.flush();
        var resposta = resultado(c);
        auditoria.registrar(
                "CARGA_INICIAL",
                id,
                "CANCELAMENTO_CARGA",
                CadastroSupport.motivo(d.motivo()),
                antes,
                Map.of("resolucao", d, "resultado", resposta));
        operacoes.salvar(
                d.operacaoId(),
                "CANCELAMENTO_CARGA",
                c.getCliente(),
                c.getArmazem(),
                id,
                hash,
                resposta);
        return resposta;
    }

    public CargaInicialDto.Resultado consultar(@NotNull @Positive Long id) {
        return resultado(obter(id));
    }

    public PaginaResponse<CargaInicialDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            int pagina,
            int tamanho) {
        return listar(clienteId, armazemId, null, null, null, pagina, tamanho);
    }

    public PaginaResponse<CargaInicialDto.Resultado> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @Positive Long produtoId,
            String referencia,
            CargaInicialDto.FiltroSituacao situacao,
            int pagina,
            int tamanho) {
        acesso.exigirSupervisor();
        autorizar(clienteId, armazemId);
        return PaginaResponse.de(
                cargas.consultar(
                        clienteId,
                        armazemId,
                        produtoId,
                        referencia == null ? null : CadastroSupport.codigo(referencia),
                        situacao == null ? null : SituacaoCargaInicial.valueOf(situacao.name()),
                        CadastroSupport.pagina(pagina, tamanho)),
                this::resultado);
    }

    public PaginaResponse<CargaInicialDto.Revisao> revisoes(
            @NotNull @Positive Long id, int pagina, int tamanho) {
        obter(id);
        return PaginaResponse.de(
                revisoes.findByCargaId(id, CadastroSupport.pagina(pagina, tamanho)), this::revisao);
    }

    private EntradaConferida efetivar(
            CargaInicial c, CargaInicialDto.Dados d, UUID operacao, String motivo) {
        if (d.referenciaPedido() == null
                || d.referenciaPedido().isBlank()
                || d.nota() == null
                || d.chegadaReal() == null
                || d.dataFifo() == null
                || !d.chegadaReal().equals(d.dataFifo())
                || d.quantidadeBoa() == null
                || d.quantidadeAvariada() == null
                || d.quantidadeBoa().add(d.quantidadeAvariada()).compareTo(c.getQuantidade()) != 0)
            throw conflito(
                    "ORIGEM_PENDENTE",
                    "Faltam nota fornecida, chegada/FIFO comprovados iguais e quantidades completas para efetivar.");
        if (d.nota().itens().size() != 1
                || !d.nota().itens().getFirst().produtoId().equals(c.getProduto().getId())
                || d.nota().itens().getFirst().quantidadePrevista().compareTo(c.getQuantidade())
                        != 0)
            throw CadastroSupport.invalido(
                    "Regularização deste estágio exige uma origem/produto/quantidade exatos.");
        var p =
                pedidos.criar(
                        new PedidoEntradaDto.Criar(
                                c.getCliente().getId(),
                                c.getArmazem().getId(),
                                d.referenciaPedido()));
        var n = d.nota();
        p =
                notas.adicionar(
                        p.id(),
                        new PedidoEntradaDto.NotaManual(
                                p.versao(),
                                n.serie(),
                                n.numero(),
                                n.emissao(),
                                n.chaveAcesso(),
                                n.itens()));
        p = pedidos.iniciar(p.id(), new RevisaoCadastroRequest(p.versao(), motivo));
        var item = pedidos.consultar(p.id()).notas().getFirst().itens().getFirst();
        p =
                recebimento.registrarChegada(
                        p.id(),
                        new RecebimentoDto.RegistrarChegada(
                                p.versao(),
                                chave(operacao, "CHEGADA"),
                                d.chegadaReal(),
                                motivo,
                                List.of(
                                        new RecebimentoDto.Item(
                                                item.id(),
                                                d.lote(),
                                                d.validade(),
                                                d.quantidadeBoa(),
                                                d.quantidadeAvariada()))));
        recebimento.efetivar(p.id(), new PedidoEntradaDto.Efetivar(p.versao(), false, motivo));
        var es = entradas.findByItemChegadaChegadaPedidoId(p.id(), CadastroSupport.pagina(0, 2));
        if (es.getTotalElements() != 1)
            throw conflito(
                    "ORIGEM_DIVERGENTE",
                    "Regularização precisa produzir exatamente uma entrada conferida.");
        return es.getContent().getFirst();
    }

    private CargaInicial bloquear(Long id) {
        var c = obter(id);
        var r = atual(c);
        var d = mapper.readValue(r.getDadosJson(), CargaInicialDto.Dados.class);
        if (d.entradaExistenteId() != null) {
            var e =
                    entradas.findById(d.entradaExistenteId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            pedidos.bloquear(e.getItemChegada().getChegada().getPedido().getId());
        }
        clientes.buscarParaAtualizar(c.getCliente().getId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        armazens.buscarParaAtualizar(c.getArmazem().getId())
                .orElseThrow(RegraNegocioException::naoEncontrado);
        em.refresh(c);
        if (c.getCliente().getSituacao() == SituacaoCadastro.INATIVO
                || c.getArmazem().getSituacao() == SituacaoCadastro.INATIVO
                || c.getProduto().getSituacao() == SituacaoCadastro.INATIVO)
            throw conflito(
                    "CADASTRO_INATIVO", "Estágio de cadastro inativo não admite operação física.");
        if (c.getCliente().getSituacao() != SituacaoCadastro.ATIVO
                || c.getArmazem().getSituacao() != SituacaoCadastro.ATIVO
                || c.getProduto().getSituacao() != SituacaoCadastro.ATIVO) acesso.exigirGestor();
        return c;
    }

    private CargaInicial obter(Long id) {
        acesso.exigirSupervisor();
        var c = cargas.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        autorizar(c.getCliente().getId(), c.getArmazem().getId());
        return c;
    }

    private void conferir(CargaInicial c, long versao) {
        if (c.getVersao() != versao || c.getSituacao() != SituacaoCargaInicial.PENDENTE)
            throw conflito(
                    "ESTAGIO_DESATUALIZADO", "Consulte o estágio pendente e sua revisão atual.");
    }

    private void conferirEntrada(CargaInicial c, EntradaConferida e) {
        var i = e.getItemChegada();
        var p = i.getChegada().getPedido();
        if (!p.getCliente().getId().equals(c.getCliente().getId())
                || !p.getArmazem().getId().equals(c.getArmazem().getId())
                || !i.getItemNota().getProduto().getId().equals(c.getProduto().getId())
                || p.getSituacao() != SituacaoPedidoEntrada.EFETIVADO
                || e.getQuantidadeTriagem()
                                .add(e.getQuantidadeQuarentena())
                                .compareTo(c.getQuantidade())
                        != 0)
            throw conflito(
                    "ORIGEM_DIVERGENTE",
                    "Entrada não comprova contexto/produto/quantidade do estágio.");
    }

    private void revisar(CargaInicial c, CargaInicialDto.Dados d, String motivo) {
        c.revisar(agora());
        String json = mapper.writeValueAsString(d);
        revisoes.saveAndFlush(
                new RevisaoCargaInicial(
                        c,
                        json,
                        NfeXmlService.hash(json),
                        acesso.usuario(),
                        CadastroSupport.motivo(motivo),
                        agora()));
        cargas.flush();
    }

    private RevisaoCargaInicial atual(CargaInicial c) {
        return revisoes.findByCargaIdAndNumero(c.getId(), c.getRevisaoAtual())
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private CargaInicialDto.Revisao revisao(RevisaoCargaInicial r) {
        return new CargaInicialDto.Revisao(
                r.getNumero(),
                r.getConteudoHash(),
                mapper.readValue(r.getDadosJson(), CargaInicialDto.Dados.class));
    }

    private CargaInicialDto.Resultado resultado(CargaInicial c) {
        var d = revisao(atual(c));
        var p = new ArrayList<String>();
        if (c.getSituacao() == SituacaoCargaInicial.PENDENTE) {
            if (d.dados().entradaExistenteId() == null && d.dados().nota() == null)
                p.add("ORIGEM_DESCONHECIDA");
            if (d.dados().entradaExistenteId() == null
                    && (d.dados().chegadaReal() == null || d.dados().dataFifo() == null))
                p.add("DATAS_FISICAS_DESCONHECIDAS");
            p.add("CONFERENCIA_PENDENTE");
        }
        if (c.getSituacao() == SituacaoCargaInicial.PREPARADA) p.add("CONFERENCIA_PENDENTE");
        return new CargaInicialDto.Resultado(
                c.getId(),
                c.getVersao(),
                c.getCliente().getId(),
                c.getArmazem().getId(),
                c.getProduto().getId(),
                c.getReferencia(),
                c.getEtiquetaFornecida(),
                c.getSituacao() == SituacaoCargaInicial.PENDENTE
                        ? c.getQuantidade()
                        : BigDecimal.ZERO,
                c.getSituacao(),
                d,
                c.getEntrada() == null ? null : c.getEntrada().getId(),
                c.getEntrada() == null ? List.of() : etiquetas(c.getEntrada().getId()),
                List.copyOf(p));
    }

    private List<ConteudoUnidade> conteudosDaEntrada(Long id) {
        return em.createQuery(
                        "select c from ConteudoUnidade c where c.entrada.id=:id order by c.unidade.id",
                        ConteudoUnidade.class)
                .setParameter("id", id)
                .getResultList();
    }

    private List<String> etiquetas(Long id) {
        return conteudosDaEntrada(id).stream()
                .filter(c -> c.getQuantidade().signum() > 0)
                .map(c -> c.getUnidade().getCodigo())
                .distinct()
                .toList();
    }

    private void salvar(
            CargaInicial c,
            String acao,
            UUID id,
            String hash,
            String motivo,
            Object antes,
            CargaInicialDto.Resultado depois) {
        auditoria.registrar(
                "CARGA_INICIAL", c.getId(), acao, CadastroSupport.motivo(motivo), antes, depois);
        operacoes.salvar(id, acao, c.getCliente(), c.getArmazem(), c.getId(), hash, depois);
    }

    private void autorizar(Long cliente, Long armazem) {
        acesso.cliente(cliente);
        acesso.armazem(armazem);
    }

    private static UUID chave(UUID operacao, String etapa) {
        return UUID.nameUUIDFromBytes(
                (operacao + ":" + etapa).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    private static RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }
}
