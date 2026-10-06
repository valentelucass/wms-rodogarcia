package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.CapacidadeDto;
import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.EntradaConferidaRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.ReservaSaidaRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class EstoqueService {
    private final UnidadeLogisticaRepository unidades;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final EntradaConferidaRepository entradas;
    private final MovimentoEstoqueRepository movimentos;
    private final ProdutoRepository produtos;
    private final ReservaSaidaRepository reservas;
    private final PedidoEntradaService pedidos;
    private final AcessoService acesso;
    private final JsonMapper mapper;

    public EstoqueService(
            UnidadeLogisticaRepository unidades,
            OcupacaoEnderecoRepository ocupacoes,
            EntradaConferidaRepository entradas,
            MovimentoEstoqueRepository movimentos,
            ProdutoRepository produtos,
            ReservaSaidaRepository reservas,
            PedidoEntradaService pedidos,
            AcessoService acesso,
            JsonMapper mapper) {
        this.unidades = unidades;
        this.ocupacoes = ocupacoes;
        this.entradas = entradas;
        this.movimentos = movimentos;
        this.produtos = produtos;
        this.reservas = reservas;
        this.pedidos = pedidos;
        this.acesso = acesso;
        this.mapper = mapper;
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<Long> disponiveis(List<Long> ids) {
        return ids.isEmpty() ? Set.of() : Set.copyOf(unidades.buscarDisponiveis(ids));
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<Long> elegiveis(List<Long> ids) {
        return ids.isEmpty() ? Set.of() : Set.copyOf(unidades.buscarElegiveis(ids));
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public Set<Long> elegiveisSaida(
            List<Long> ids,
            Long pedidoId,
            br.com.rodogarcia.wms.models.TipoEndereco tipo,
            boolean resolverPendentes) {
        if (resolverPendentes) acesso.exigirGestor();
        var situacoes =
                resolverPendentes
                        ? List.of(
                                br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO,
                                br.com.rodogarcia.wms.models.SituacaoCadastro.ENCERRAMENTO_PENDENTE)
                        : List.of(br.com.rodogarcia.wms.models.SituacaoCadastro.ATIVO);
        return ids.isEmpty()
                ? Set.of()
                : Set.copyOf(unidades.buscarElegiveisSaida(ids, pedidoId, tipo, situacoes));
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public List<UnidadeLogistica> candidatas(
            Long clienteId, Long armazemId, List<Long> produtosIds) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
        return produtosIds.isEmpty()
                ? List.of()
                : unidades.buscarCandidatas(clienteId, armazemId, produtosIds);
    }

    @Transactional(propagation = Propagation.MANDATORY, readOnly = true)
    public EstoqueDto.Unidade detalhe(UnidadeLogistica unidade) {
        return mapear(
                unidade,
                disponiveis(List.of(unidade.getId())).contains(unidade.getId()),
                ocupacoes.buscarDasUnidades(List.of(unidade.getId())));
    }

    public EstoqueDto.Unidade consultar(@NotNull UUID codigo) {
        return detalhe(obter(codigo));
    }

    public PaginaResponse<EstoqueDto.Unidade> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @Positive Long produtoId,
            Boolean disponivel,
            int pagina,
            int tamanho) {
        return listar(
                clienteId, armazemId, produtoId, null, null, disponivel, null, pagina, tamanho);
    }

    public PaginaResponse<EstoqueDto.Unidade> listar(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @Positive Long produtoId,
            UUID codigo,
            @Positive Long enderecoId,
            Boolean disponivel,
            String situacao,
            int pagina,
            int tamanho) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
        if (situacao != null
                && !Set.of(
                                "DISPONIVEL",
                                "RESERVADO",
                                "BLOQUEADO",
                                "CONTAGEM_PENDENTE",
                                "CONFERENCIA_PENDENTE",
                                "TRIAGEM",
                                "QUARENTENA",
                                "ARMAZENAGEM",
                                "SEPARACAO")
                        .contains(situacao))
            throw CadastroSupport.invalido("Situação de estoque inválida.");
        var resultado =
                unidades.filtrarEstoque(
                        clienteId,
                        armazemId,
                        produtoId,
                        codigo == null ? null : codigo.toString(),
                        enderecoId,
                        disponivel,
                        situacao,
                        CadastroSupport.pagina(pagina, tamanho));
        var ids = resultado.getContent().stream().map(UnidadeLogistica::getId).toList();
        var liberadas = disponiveis(ids);
        Map<Long, List<OcupacaoEndereco>> porUnidade =
                ids.isEmpty()
                        ? Map.of()
                        : ocupacoes.buscarDasUnidades(ids).stream()
                                .collect(Collectors.groupingBy(o -> o.getUnidade().getId()));
        return PaginaResponse.de(
                resultado,
                u ->
                        mapear(
                                u,
                                liberadas.contains(u.getId()),
                                porUnidade.getOrDefault(u.getId(), List.of())));
    }

    @Transactional(readOnly = true, isolation = Isolation.SERIALIZABLE)
    public EstoqueDto.Saldo saldo(
            @NotNull @Positive Long clienteId,
            @NotNull @Positive Long armazemId,
            @NotNull @Positive Long produtoId) {
        acesso.cliente(clienteId);
        acesso.armazem(armazemId);
        var produto =
                produtos.findById(produtoId).orElseThrow(RegraNegocioException::naoEncontrado);
        if (!produto.getCliente().getId().equals(clienteId))
            throw RegraNegocioException.naoEncontrado();
        var total = unidades.resumirEstoque(clienteId, armazemId, produtoId);
        var pendente = zero(entradas.quantidadePendenteUnitizacao(clienteId, armazemId, produtoId));
        var fisico = zero(total.getFisico());
        var disponivel = zero(unidades.somarDisponivel(clienteId, armazemId, produtoId));
        var reservado = zero(reservas.somarAtiva(clienteId, armazemId, produtoId));
        return new EstoqueDto.Saldo(
                clienteId,
                armazemId,
                produtoId,
                produto.getUnidadeMedida(),
                fisico.add(pendente),
                pendente,
                fisico,
                disponivel,
                reservado,
                fisico.subtract(disponivel).subtract(reservado),
                zero(total.getNaoEnderecado()),
                zero(total.getTriagem()),
                zero(total.getQuarentena()),
                zero(total.getArmazenagem()),
                zero(total.getAvariado()));
    }

    public PaginaResponse<EstoqueDto.Movimento> historico(
            @NotNull UUID codigo, int pagina, int tamanho) {
        var unidade = obter(codigo);
        return PaginaResponse.de(
                movimentos.findByUnidadeId(
                        unidade.getId(), CadastroSupport.pagina(pagina, tamanho)),
                m ->
                        new EstoqueDto.Movimento(
                                m.getId(),
                                m.getOperacaoId(),
                                m.getAcao(),
                                m.getUsuario(),
                                m.getMotivo(),
                                m.getInstante(),
                                mapper.readValue(m.getDadosAntes(), EstoqueDto.Unidade.class),
                                mapper.readValue(m.getResultado(), EstoqueDto.Confirmacao.class)));
    }

    private UnidadeLogistica obter(UUID codigo) {
        Long pedidoId =
                unidades.buscarPedidoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        pedidos.obter(pedidoId);
        return unidades.findByCodigo(codigo.toString())
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private EstoqueDto.Unidade mapear(
            UnidadeLogistica u, boolean disponivel, List<OcupacaoEndereco> ocupacoes) {
        var posicoes =
                ocupacoes.stream()
                        .map(
                                o -> {
                                    var e = o.getEndereco();
                                    return new EstoqueDto.Posicao(
                                            e.getId(),
                                            e.getCodigo(),
                                            e.getTipo(),
                                            e.getSituacao(),
                                            new CapacidadeDto.Limites(
                                                    e.getCapacidadePesoKg(),
                                                    e.getAlturaMetros(),
                                                    e.getLarguraMetros(),
                                                    e.getProfundidadeMetros(),
                                                    e.getEmpilhamentoMaximo()));
                                })
                        .toList();
        return new EstoqueDto.Unidade(
                UnidadeLogisticaDto.Resumo.de(u, disponivel),
                EstoqueDto.Medidas.de(u.getMedidas()),
                u.getTipoLocalizacao(),
                u.isBloqueada(),
                u.isAvariaPosterior(),
                u.isAvariaInicialReparada(),
                u.getPrimeiroEnderecamentoEm(),
                u.getInicioArmazenagemEm(),
                u.getPosicoesEquivalentes(),
                u.getConjuntoAtual() == null ? null : u.getConjuntoAtual().getId(),
                posicoes);
    }

    private BigDecimal zero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
