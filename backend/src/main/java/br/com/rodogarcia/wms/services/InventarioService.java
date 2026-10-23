package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.InventarioItem;
import br.com.rodogarcia.wms.models.InventarioSessao;
import br.com.rodogarcia.wms.models.TipoInventario;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.InventarioRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class InventarioService {
    private final InventarioRepository inventarios;
    private final UnidadeLogisticaRepository unidades;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final ProdutoRepository produtos;
    private final EnderecoRepository enderecos;
    private final ClienteRepository clientes;
    private final ArmazemRepository armazens;
    private final AcessoService acesso;
    private final OperacaoAdministrativaService operacoes;
    private final AuditoriaService auditoria;
    private final JsonMapper mapper;
    private final Clock clock;
    private final boolean habilitado;
    private static final int LIMITE = 20000;

    public InventarioService(
            InventarioRepository inventarios,
            UnidadeLogisticaRepository unidades,
            OcupacaoEnderecoRepository ocupacoes,
            ProdutoRepository produtos,
            EnderecoRepository enderecos,
            ClienteRepository clientes,
            ArmazemRepository armazens,
            AcessoService acesso,
            OperacaoAdministrativaService operacoes,
            AuditoriaService auditoria,
            JsonMapper mapper,
            Clock clock,
            @Value("${wms.inventario.enabled:false}") boolean habilitado) {
        this.inventarios = inventarios;
        this.unidades = unidades;
        this.ocupacoes = ocupacoes;
        this.produtos = produtos;
        this.enderecos = enderecos;
        this.clientes = clientes;
        this.armazens = armazens;
        this.acesso = acesso;
        this.operacoes = operacoes;
        this.auditoria = auditoria;
        this.mapper = mapper;
        this.clock = clock;
        this.habilitado = habilitado;
    }

    public InventarioDto.Disponibilidade disponibilidade() {
        acesso.usuario();
        return new InventarioDto.Disponibilidade(
                habilitado,
                habilitado ? null : "O registro de inventários aguarda ativação neste ambiente.");
    }

    private void exigirAtivacao() {
        if (!habilitado)
            throw RegraNegocioException.conflito(
                    "INVENTARIO_NAO_ATIVADO",
                    "O registro de inventários aguarda ativação neste ambiente.");
    }

    private void autorizar(Long cliente, Long armazem) {
        acesso.cliente(cliente);
        acesso.armazem(armazem);
    }

    public PaginaResponse<InventarioDto.Visao> visao(
            Long clienteId,
            Long armazemId,
            Long produtoId,
            Long enderecoId,
            String pesquisa,
            String agrupar,
            int pagina,
            int tamanho) {
        autorizar(clienteId, armazemId);
        CadastroSupport.pagina(pagina, tamanho);
        if (!List.of("SKU", "POSICAO").contains(agrupar))
            throw CadastroSupport.invalido("Visão inválida.");
        if (pesquisa != null && pesquisa.length() > 100)
            throw CadastroSupport.invalido("Pesquise até 100 caracteres.");
        return inventarios.visao(
                clienteId,
                armazemId,
                produtoId,
                enderecoId,
                pesquisa == null ? "" : pesquisa.strip(),
                "POSICAO".equals(agrupar),
                pagina,
                tamanho);
    }

    public PaginaResponse<InventarioDto.Resumo> listar(
            Long clienteId, Long armazemId, int pagina, int tamanho) {
        exigirAtivacao();
        autorizar(clienteId, armazemId);
        CadastroSupport.pagina(pagina, tamanho);
        var dados = inventarios.listar(clienteId, armazemId, pagina, tamanho);
        return new PaginaResponse<>(
                dados.itens().stream().map(this::resumo).toList(),
                pagina,
                tamanho,
                dados.totalItens(),
                dados.totalPaginas());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InventarioDto.Resumo iniciar(@NotNull @Valid InventarioDto.Iniciar d) {
        exigirAtivacao();
        autorizar(d.clienteId(), d.armazemId());
        String hash = operacoes.hash("INICIO_INVENTARIO", List.of(d.clienteId(), d.armazemId()), d);
        var repetida = operacoes.repetida(d.operacaoId(), hash, InventarioDto.Resumo.class);
        if (repetida != null) return repetida;
        var cliente =
                clientes.findById(d.clienteId()).orElseThrow(RegraNegocioException::naoEncontrado);
        var armazem =
                armazens.findById(d.armazemId()).orElseThrow(RegraNegocioException::naoEncontrado);
        CadastroSupport.ativo(cliente);
        CadastroSupport.ativo(armazem);
        CadastroSupport.motivo(d.motivo());
        if ((d.tipo() == TipoInventario.SKU) != (d.produtoId() != null)
                || (d.tipo() == TipoInventario.POSICAO) != (d.enderecoId() != null))
            throw CadastroSupport.invalido(
                    "Selecione somente o SKU ou a posição exigida pelo tipo de inventário.");
        if (d.produtoId() != null) {
            var produto =
                    produtos.findById(d.produtoId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            if (!Objects.equals(produto.getCliente().getId(), d.clienteId()))
                throw RegraNegocioException.naoEncontrado();
        }
        validarEndereco(d.enderecoId(), d.armazemId());
        var bases = bases(d.clienteId(), d.armazemId(), d.produtoId(), d.enderecoId());
        Long id = inventarios.criar(d, acesso.usuario(), agora());
        inventarios.guardarBase(id, bases);
        var resposta = resumo(inventarios.consultar(id, false));
        auditoria.registrar("INVENTARIO", id, "CRIACAO", d.motivo(), null, resposta);
        operacoes.salvar(d.operacaoId(), "INICIO_INVENTARIO", cliente, armazem, id, hash, resposta);
        return resposta;
    }

    private InventarioSessao consultar(Long id, boolean bloquear) {
        exigirAtivacao();
        var i = inventarios.consultar(id, false);
        if (i == null) throw RegraNegocioException.naoEncontrado();
        autorizar(i.clienteId(), i.armazemId());
        return bloquear ? inventarios.consultar(id, true) : i;
    }

    public InventarioDto.Detalhe consultar(Long id) {
        var i = consultar(id, false);
        return new InventarioDto.Detalhe(
                resumo(i), inventarios.ultimasLeituras(id).stream().map(this::leitura).toList());
    }

    @Transactional
    public InventarioDto.Registro ler(Long id, @NotNull @Valid InventarioDto.Ler d) {
        var i = consultar(id, true);
        String hash = operacoes.hash("LEITURA_INVENTARIO", id, d);
        var repetida = operacoes.repetida(d.operacaoId(), hash, InventarioDto.Registro.class);
        if (repetida != null) return repetida;
        validarAberto(i, d.versao());
        var codigo = d.codigoUnidade().toString();
        var escopo =
                unidades.buscarEscopoPorCodigo(codigo)
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        if (!Objects.equals(escopo.getClienteId(), i.clienteId())
                || !Objects.equals(escopo.getArmazemId(), i.armazemId()))
            throw RegraNegocioException.naoEncontrado();
        var u = unidades.findByCodigo(codigo).orElseThrow(RegraNegocioException::naoEncontrado);
        if (i.tipo() == TipoInventario.SKU
                && !Objects.equals(u.getProduto().getId(), i.produtoId()))
            throw CadastroSupport.invalido(
                    "Esta mercadoria não pertence ao SKU selecionado para o inventário.");
        if (i.tipo() == TipoInventario.POSICAO && !Objects.equals(d.enderecoId(), i.enderecoId()))
            throw CadastroSupport.invalido("Confirme a posição selecionada para este inventário.");
        if (d.contado().signum() < 0
                || d.contado().stripTrailingZeros().scale()
                        > u.getProduto().getPrecisaoQuantidade())
            throw CadastroSupport.invalido("Quantidade incompatível com a precisão do produto.");
        var posicao = validarEndereco(d.enderecoId(), i.armazemId());
        var anterior = inventarios.item(id, codigo);
        if (anterior != null && anterior.contado() != null && !d.substituir())
            throw RegraNegocioException.conflito(
                    "ETIQUETA_JA_LIDA",
                    "Etiqueta já contada. Use Recontar para substituir a leitura, sem somar novamente.");
        if (anterior == null && inventarios.totalMercadorias(id) >= LIMITE)
            throw CadastroSupport.invalido(
                    "Limite de mercadorias atingido. Separe os próximos inventários por SKU ou posição.");
        var base =
                anterior == null
                        ? base(u, posicoes(List.of(u.getId())).getOrDefault(u.getId(), List.of()))
                        : anterior.base();
        inventarios.ler(id, base, d, posicao, acesso.usuario(), agora());
        var resposta =
                new InventarioDto.Registro(
                        resumo(inventarios.consultar(id, false)),
                        leitura(inventarios.item(id, codigo)));
        auditoria.registrar(
                "INVENTARIO",
                id,
                "ALTERACAO",
                d.substituir() ? "Recontagem da mercadoria" : "Leitura da mercadoria",
                anterior == null || anterior.contado() == null ? null : leitura(anterior),
                resposta);
        salvar(i, d.operacaoId(), "LEITURA_INVENTARIO", hash, resposta);
        return resposta;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InventarioDto.Relatorio finalizar(Long id, @NotNull @Valid InventarioDto.Encerrar d) {
        var i = consultar(id, true);
        String hash = operacoes.hash("FIM_INVENTARIO", id, d);
        var repetida = operacoes.repetida(d.operacaoId(), hash, InventarioDto.Relatorio.class);
        if (repetida != null) return repetida;
        validarAberto(i, d.versao());
        if (!d.confirmarNaoLidas())
            throw CadastroSupport.invalido(
                    "Confirme que as mercadorias não lidas serão tratadas como faltantes.");
        CadastroSupport.motivo(d.motivo());
        var itens = inventarios.itens(id);
        var atuais = bases(i.clienteId(), i.armazemId(), i.produtoId(), i.enderecoId());
        var linhas = InventarioConferencia.conferir(itens, atuais);
        int divergencias = (int) linhas.stream().filter(l -> !l.ocorrencias().isEmpty()).count();
        var encerrado = agora();
        var resumo =
                new InventarioDto.Resumo(
                        i.id(),
                        i.versao() + 1,
                        i.clienteId(),
                        i.armazemId(),
                        i.tipo(),
                        i.produtoId(),
                        i.enderecoId(),
                        "CONCLUIDO",
                        i.iniciadoEm(),
                        encerrado,
                        i.usuario(),
                        i.unidadesLidas(),
                        divergencias);
        var resposta =
                new InventarioDto.Relatorio(
                        resumo,
                        i.iniciadoEm(),
                        (int) i.unidadesLidas(),
                        contar(linhas, "NAO_LIDA"),
                        contar(linhas, "QUANTIDADE"),
                        contar(linhas, "ENDERECO"),
                        contar(linhas, "MOVIMENTACAO"),
                        InventarioConferencia.porSku(linhas),
                        linhas);
        inventarios.encerrar(id, "CONCLUIDO", encerrado, divergencias, resposta);
        auditoria.registrar("INVENTARIO", id, "ALTERACAO", d.motivo(), resumo(i), resumo);
        salvar(i, d.operacaoId(), "FIM_INVENTARIO", hash, resposta);
        return resposta;
    }

    @Transactional
    public InventarioDto.Resumo cancelar(Long id, @NotNull @Valid InventarioDto.Encerrar d) {
        acesso.exigirSupervisor();
        var i = consultar(id, true);
        var hash = operacoes.hash("CANCELAMENTO_INVENTARIO", id, d);
        var repetida = operacoes.repetida(d.operacaoId(), hash, InventarioDto.Resumo.class);
        if (repetida != null) return repetida;
        validarAberto(i, d.versao());
        CadastroSupport.motivo(d.motivo());
        inventarios.encerrar(id, "CANCELADO", agora(), null, null);
        var resposta = resumo(inventarios.consultar(id, false));
        auditoria.registrar("INVENTARIO", id, "ALTERACAO", d.motivo(), resumo(i), resposta);
        salvar(i, d.operacaoId(), "CANCELAMENTO_INVENTARIO", hash, resposta);
        return resposta;
    }

    public InventarioDto.Relatorio relatorio(Long id) {
        var i = consultar(id, false);
        if (!"CONCLUIDO".equals(i.situacao()))
            throw RegraNegocioException.conflito(
                    "INVENTARIO_ABERTO", "O relatório fica disponível após encerrar o inventário.");
        return mapper.readValue(inventarios.relatorio(id), InventarioDto.Relatorio.class);
    }

    private int contar(List<InventarioDto.Linha> linhas, String ocorrencia) {
        return (int) linhas.stream().filter(l -> l.ocorrencias().contains(ocorrencia)).count();
    }

    private void salvar(
            InventarioSessao i,
            java.util.UUID operacao,
            String tipo,
            String hash,
            Object resposta) {
        operacoes.salvar(
                operacao,
                tipo,
                clientes.getReferenceById(i.clienteId()),
                armazens.getReferenceById(i.armazemId()),
                i.id(),
                hash,
                resposta);
    }

    private void validarAberto(InventarioSessao i, Long versao) {
        if (!"EM_ANDAMENTO".equals(i.situacao()))
            throw RegraNegocioException.conflito(
                    "INVENTARIO_ENCERRADO", "O inventário já foi encerrado.");
        if (i.versao() != versao)
            throw RegraNegocioException.conflito(
                    "VERSAO_DESATUALIZADA",
                    "Outra leitura atualizou o inventário. Recarregue antes de confirmar.");
    }

    private String validarEndereco(Long id, Long armazem) {
        if (id == null) return null;
        var endereco = enderecos.findById(id).orElseThrow(RegraNegocioException::naoEncontrado);
        if (!Objects.equals(endereco.getArmazem().getId(), armazem))
            throw RegraNegocioException.naoEncontrado();
        return endereco.getCodigo();
    }

    private Map<Long, List<InventarioItem.Posicao>> posicoes(List<Long> ids) {
        return ocupacoes.buscarDasUnidades(ids).stream()
                .collect(
                        Collectors.groupingBy(
                                o -> o.getUnidade().getId(),
                                Collectors.mapping(
                                        o ->
                                                new InventarioItem.Posicao(
                                                        o.getEndereco().getId(),
                                                        o.getEndereco().getCodigo()),
                                        Collectors.toList())));
    }

    private InventarioItem.Base base(UnidadeLogistica u, List<InventarioItem.Posicao> posicoes) {
        var p = u.getProduto();
        return new InventarioItem.Base(
                u.getId(),
                u.getCodigo(),
                u.getVersao(),
                p.getId(),
                p.getSku(),
                p.getDescricao(),
                p.getUnidadeMedida(),
                p.getPrecisaoQuantidade(),
                u.getQuantidade(),
                List.copyOf(posicoes));
    }

    private List<InventarioItem.Base> bases(
            Long cliente, Long armazem, Long produto, Long endereco) {
        var resultado = new ArrayList<InventarioItem.Base>();
        for (int pagina = 0; ; pagina++) {
            var lote =
                    unidades.filtrarEstoque(
                            cliente,
                            armazem,
                            produto,
                            null,
                            endereco,
                            null,
                            null,
                            PageRequest.of(pagina, 500, Sort.by("id")));
            if (lote.getTotalElements() > LIMITE)
                throw CadastroSupport.invalido(
                        "O escopo excede 20.000 mercadorias. Divida o inventário por SKU ou posição.");
            if (!lote.isEmpty()) {
                var locais =
                        posicoes(lote.getContent().stream().map(UnidadeLogistica::getId).toList());
                lote.forEach(
                        u -> resultado.add(base(u, locais.getOrDefault(u.getId(), List.of()))));
            }
            if (!lote.hasNext()) return List.copyOf(resultado);
        }
    }

    private InventarioDto.Resumo resumo(InventarioSessao i) {
        return new InventarioDto.Resumo(
                i.id(),
                i.versao(),
                i.clienteId(),
                i.armazemId(),
                i.tipo(),
                i.produtoId(),
                i.enderecoId(),
                i.situacao(),
                i.iniciadoEm(),
                i.encerradoEm(),
                i.usuario(),
                i.unidadesLidas(),
                i.divergencias());
    }

    private InventarioDto.Leitura leitura(InventarioItem i) {
        return new InventarioDto.Leitura(
                i.base().codigoUnidade(),
                i.base().sku(),
                i.base().descricao(),
                i.base().unidadeMedida(),
                i.contado(),
                i.enderecoLido(),
                i.posicaoLida(),
                i.lidoEm());
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
