package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.ConjuntoPosicoes;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.MedidasUnidade;
import br.com.rodogarcia.wms.models.MovimentoEstoque;
import br.com.rodogarcia.wms.models.OcupacaoEndereco;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoPedidoEntrada;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ConjuntoPosicoesRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.MovimentoEstoqueRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class MovimentacaoEstoqueService {
    private final PedidoEntradaService pedidos;
    private final UnidadeLogisticaRepository unidades;
    private final EnderecoRepository enderecos;
    private final ConjuntoPosicoesRepository conjuntos;
    private final OcupacaoEnderecoRepository ocupacoes;
    private final MovimentoEstoqueRepository movimentos;
    private final ArmazemRepository armazens;
    private final EstoqueService estoque;
    private final AcessoService acesso;
    private final JsonMapper mapper;
    private final Clock clock;
    private final HistoricoOperacaoTemporalService temporal;

    public MovimentacaoEstoqueService(
            PedidoEntradaService pedidos,
            UnidadeLogisticaRepository unidades,
            EnderecoRepository enderecos,
            ConjuntoPosicoesRepository conjuntos,
            OcupacaoEnderecoRepository ocupacoes,
            MovimentoEstoqueRepository movimentos,
            ArmazemRepository armazens,
            EstoqueService estoque,
            AcessoService acesso,
            JsonMapper mapper,
            Clock clock,
            HistoricoOperacaoTemporalService temporal) {
        this.pedidos = pedidos;
        this.unidades = unidades;
        this.enderecos = enderecos;
        this.conjuntos = conjuntos;
        this.ocupacoes = ocupacoes;
        this.movimentos = movimentos;
        this.armazens = armazens;
        this.estoque = estoque;
        this.acesso = acesso;
        this.mapper = mapper;
        this.clock = clock;
        this.temporal = temporal;
    }

    @Transactional
    public EstoqueDto.Confirmacao posicionar(
            @NotNull UUID codigo, @NotNull @Valid EstoqueDto.Posicionar dados) {
        return posicionarDatado(codigo, dados, null, null);
    }

    @Transactional
    public EstoqueDto.Confirmacao posicionarContingencia(
            @NotNull UUID codigo,
            @NotNull @Valid EstoqueDto.Posicionar dados,
            @NotNull Instant ocorridaEm,
            @NotNull String identidadeFato) {
        acesso.exigirSupervisor();
        return posicionarDatado(codigo, dados, ocorridaEm, identidadeFato);
    }

    private EstoqueDto.Confirmacao posicionarDatado(
            UUID codigo, EstoqueDto.Posicionar dados, Instant ocorridaEm, String identidadeFato) {
        var p = bloquearPedido(codigo);
        String hash =
                hash(
                        "POSICIONAR",
                        codigo,
                        ocorridaEm == null ? dados : List.of(dados, ocorridaEm, identidadeFato));
        var repetida = repetida(p.getId(), dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        PedidoEntradaService.exigirSituacao(p, SituacaoPedidoEntrada.EFETIVADO);
        pedidos.validarVinculos(p);
        var u = obter(codigo);
        validarUnidade(u, dados.versaoUnidade());
        if (ocorridaEm != null) temporal.conferir(u, ocorridaEm);
        if (u.getTipoLocalizacao() == TipoEndereco.SEPARACAO)
            throw RegraNegocioException.conflito(
                    "FLUXO_SAIDA_NECESSARIO",
                    "Unidade separada exige retorno ou retirada pelo fluxo de saída.");
        var medidas = u.getMedidas();
        if (medidas == null) {
            if (dados.medidas() == null)
                throw CadastroSupport.invalido(
                        "Informe as medidas físicas no primeiro endereçamento.");
            var m = dados.medidas();
            medidas =
                    new MedidasUnidade(
                            m.pesoKg(),
                            m.alturaMetros(),
                            m.larguraMetros(),
                            m.profundidadeMetros(),
                            m.empilhamento(),
                            m.posicoesNecessarias());
        } else if (dados.medidas() != null)
            throw CadastroSupport.invalido(
                    "As medidas já estão registradas; não as substitua no remanejamento.");
        if (dados.destinos().size() != medidas.getPosicoesNecessarias())
            throw CadastroSupport.invalido("Informe todas as posições necessárias à unidade.");
        var destinosIds = new HashSet<Long>();
        for (var d : dados.destinos())
            if (!destinosIds.add(d.enderecoId()))
                throw CadastroSupport.invalido("Posições de destino repetidas.");
        var origemIds = ocupacoes.buscarEnderecos(u.getId());
        if (Set.copyOf(origemIds).equals(destinosIds))
            throw RegraNegocioException.conflito(
                    "SEM_MOVIMENTACAO", "A unidade já ocupa essas posições.");
        var todosIds = new HashSet<>(destinosIds);
        todosIds.addAll(origemIds);
        var posicoes = new HashMap<Long, Endereco>();
        // O armazém já está bloqueado. Endereços em ordem impedem disputa dos dois lugares.
        for (Long id : todosIds.stream().sorted().toList())
            posicoes.put(
                    id,
                    enderecos
                            .buscarParaAtualizar(id)
                            .orElseThrow(RegraNegocioException::naoEncontrado));
        TipoEndereco tipo = null;
        for (var d : dados.destinos()) {
            var e = posicoes.get(d.enderecoId());
            CadastroSupport.ativo(e);
            CapacidadeSupport.configurado(e);
            if (!e.getArmazem().getId().equals(p.getArmazem().getId()))
                throw CadastroSupport.invalido("Destino pertence a outro armazém.");
            if (!e.getCodigo().equals(CadastroSupport.codigo(d.codigoLido())))
                throw CadastroSupport.invalido(
                        "Código lido não corresponde ao endereço confirmado.");
            if (e.getTipoUnidadePermitido() != u.getTipo())
                throw RegraNegocioException.conflito(
                        "TIPO_INCOMPATIVEL", "Tipo de unidade incompatível com o endereço.");
            if (tipo != null && tipo != e.getTipo())
                throw CadastroSupport.invalido("As posições devem pertencer à mesma área.");
            tipo = e.getTipo();
        }
        if (tipo == TipoEndereco.SEPARACAO)
            throw RegraNegocioException.conflito(
                    "FLUXO_SAIDA_NECESSARIO",
                    "Separação exige pedido e reserva pelo fluxo de saída.");
        if (!u.isCondicaoApta() && tipo != TipoEndereco.QUARENTENA)
            throw RegraNegocioException.conflito(
                    "MERCADORIA_AVARIADA", "Mercadoria avariada deve permanecer em quarentena.");
        ConjuntoPosicoes conjunto = null;
        if (medidas.getPosicoesNecessarias() == 2) {
            if (dados.conjuntoId() == null)
                throw CadastroSupport.invalido(
                        "Selecione um conjunto compatível de duas posições.");
            conjunto =
                    conjuntos
                            .buscarParaAtualizar(dados.conjuntoId())
                            .orElseThrow(RegraNegocioException::naoEncontrado);
            CadastroSupport.ativo(conjunto);
            if (!conjunto.getArmazem().getId().equals(p.getArmazem().getId())
                    || !destinosIds.equals(
                            Set.of(
                                    conjunto.getEnderecoA().getId(),
                                    conjunto.getEnderecoB().getId())))
                throw CadastroSupport.invalido(
                        "As posições lidas não correspondem ao conjunto cadastrado.");
            CapacidadeSupport.conferir(
                    medidas,
                    conjunto.getCapacidadePesoKg(),
                    conjunto.getAlturaMetros(),
                    conjunto.getLarguraMetros(),
                    conjunto.getProfundidadeMetros(),
                    conjunto.getEmpilhamentoMaximo());
        } else {
            if (dados.conjuntoId() != null)
                throw CadastroSupport.invalido("Uma posição não utiliza conjunto duplo.");
            var e = posicoes.get(dados.destinos().get(0).enderecoId());
            CapacidadeSupport.conferir(
                    medidas,
                    e.getCapacidadePesoKg(),
                    e.getAlturaMetros(),
                    e.getLarguraMetros(),
                    e.getProfundidadeMetros(),
                    e.getEmpilhamentoMaximo());
        }
        var atuais = new HashMap<Long, OcupacaoEndereco>();
        for (Long id : todosIds.stream().sorted().toList()) {
            var ocupacao = ocupacoes.findByEnderecoId(id).orElse(null);
            if (destinosIds.contains(id)
                    && ocupacao != null
                    && ocupacao.getUnidade() != null
                    && !ocupacao.getUnidade().getId().equals(u.getId()))
                throw RegraNegocioException.conflito(
                        "POSICAO_OCUPADA", "Uma das posições já está ocupada por outra unidade.");
            atuais.put(id, ocupacao);
        }
        var antes = estoque.detalhe(u);
        var pedidoAntes = PedidoEntradaDto.Resumo.de(p);
        for (Long id : origemIds) if (!destinosIds.contains(id)) atuais.get(id).atribuir(null);
        for (Long id : destinosIds.stream().sorted().toList()) {
            var ocupacao = atuais.get(id);
            if (ocupacao == null) ocupacao = ocupacoes.save(new OcupacaoEndereco(posicoes.get(id)));
            ocupacao.atribuir(u);
        }
        boolean primeira = u.getPrimeiroEnderecamentoEm() == null;
        Instant instanteFisico =
                ocorridaEm == null ? agora() : ocorridaEm.truncatedTo(ChronoUnit.MICROS);
        u.posicionar(medidas, tipo, conjunto, instanteFisico);
        return concluirDatado(
                p,
                pedidoAntes,
                u,
                dados.operacaoId(),
                hash,
                primeira ? "ENDERECAMENTO" : "MOVIMENTACAO",
                dados.motivo(),
                antes,
                instanteFisico);
    }

    @Transactional
    public EstoqueDto.Confirmacao bloquear(
            @NotNull UUID codigo, @NotNull @Valid EstoqueDto.Bloqueio dados) {
        return alterarBloqueio(codigo, dados, true, false);
    }

    @Transactional
    public EstoqueDto.Confirmacao avariar(
            @NotNull UUID codigo, @NotNull @Valid EstoqueDto.Bloqueio dados) {
        return alterarBloqueio(codigo, dados, true, true);
    }

    @Transactional
    public EstoqueDto.Confirmacao liberar(
            @NotNull UUID codigo, @NotNull @Valid EstoqueDto.Bloqueio dados) {
        acesso.exigirSupervisor();
        return alterarBloqueio(codigo, dados, false, false);
    }

    private EstoqueDto.Confirmacao alterarBloqueio(
            UUID codigo, EstoqueDto.Bloqueio dados, boolean bloquear, boolean avaria) {
        var p = bloquearPedido(codigo);
        String acao =
                avaria ? "AVARIA_ESTOQUE" : bloquear ? "BLOQUEIO_ESTOQUE" : "LIBERACAO_ESTOQUE";
        String hash = hash(acao, codigo, dados);
        var repetida = repetida(p.getId(), dados.operacaoId(), hash);
        if (repetida != null) return repetida;
        PedidoEntradaService.exigirSituacao(p, SituacaoPedidoEntrada.EFETIVADO);
        if (bloquear)
            armazens.buscarParaAtualizar(p.getArmazem().getId())
                    .orElseThrow(RegraNegocioException::naoEncontrado);
        else pedidos.validarVinculos(p);
        var u = obter(codigo);
        validarUnidade(u, dados.versaoUnidade());
        if (avaria ? u.isAvariaPosterior() : u.isBloqueada() == bloquear)
            throw RegraNegocioException.conflito(
                    "TRANSICAO_INVALIDA", "O bloqueio já está na situação solicitada.");
        if (!bloquear
                && (u.isAvariaPosterior()
                        || !u.isCondicaoApta()
                        || u.getTipoLocalizacao() != TipoEndereco.ARMAZENAGEM))
            throw RegraNegocioException.conflito(
                    "LIBERACAO_INVALIDA",
                    "Liberação exige mercadoria boa em armazenagem; não corrige avaria nem localização.");
        if (!bloquear)
            for (Long id : ocupacoes.buscarEnderecos(u.getId()))
                CadastroSupport.ativo(
                        enderecos
                                .buscarParaAtualizar(id)
                                .orElseThrow(RegraNegocioException::naoEncontrado));
        var antes = estoque.detalhe(u);
        var pedidoAntes = PedidoEntradaDto.Resumo.de(p);
        if (avaria) u.registrarAvaria(agora());
        else u.alterarBloqueio(bloquear, agora());
        if (!bloquear) {
            unidades.flush();
            if (!estoque.elegiveis(List.of(u.getId())).contains(u.getId()))
                throw RegraNegocioException.conflito(
                        "LIBERACAO_INVALIDA", "A localização atual não permite liberar a unidade.");
        }
        return concluir(p, pedidoAntes, u, dados.operacaoId(), hash, acao, dados.motivo(), antes);
    }

    private PedidoEntrada bloquearPedido(UUID codigo) {
        var id =
                unidades.buscarPedidoPorCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        return pedidos.bloquear(id);
    }

    private UnidadeLogistica obter(UUID codigo) {
        return unidades.findByCodigo(codigo.toString())
                .orElseThrow(RegraNegocioException::naoEncontrado);
    }

    private void validarUnidade(UnidadeLogistica u, long versao) {
        if (!u.isAtiva())
            throw RegraNegocioException.conflito(
                    "UNIDADE_ENCERRADA", "Unidade sem conteúdo ativo.");
        if (u.getVersao() != versao)
            throw RegraNegocioException.conflito(
                    "VERSAO_DESATUALIZADA",
                    "Unidade alterada; consulte novamente antes de confirmar.");
    }

    private String hash(String acao, UUID codigo, Object dados) {
        return NfeXmlService.hash(acao + ":" + codigo + ":" + mapper.writeValueAsString(dados));
    }

    private EstoqueDto.Confirmacao repetida(Long pedidoId, UUID operacaoId, String hash) {
        var anterior = movimentos.findByPedidoIdAndOperacaoId(pedidoId, operacaoId.toString());
        if (anterior.isEmpty()) return null;
        if (!anterior.get().getConteudoHash().equals(hash))
            throw RegraNegocioException.conflito(
                    "OPERACAO_REUTILIZADA", "Identificador já usado com outro conteúdo.");
        return mapper.readValue(anterior.get().getResultado(), EstoqueDto.Confirmacao.class);
    }

    private EstoqueDto.Confirmacao concluir(
            PedidoEntrada p,
            PedidoEntradaDto.Resumo pedidoAntes,
            UnidadeLogistica u,
            UUID operacaoId,
            String hash,
            String acao,
            String motivo,
            EstoqueDto.Unidade antes) {
        return concluirDatado(p, pedidoAntes, u, operacaoId, hash, acao, motivo, antes, agora());
    }

    private EstoqueDto.Confirmacao concluirDatado(
            PedidoEntrada p,
            PedidoEntradaDto.Resumo pedidoAntes,
            UnidadeLogistica u,
            UUID operacaoId,
            String hash,
            String acao,
            String motivo,
            EstoqueDto.Unidade antes,
            Instant instanteFisico) {
        unidades.flush();
        var depois = estoque.detalhe(u);
        var pedido =
                pedidos.registrar(
                        p,
                        pedidoAntes,
                        acao,
                        CadastroSupport.motivo(motivo),
                        Map.of("operacaoId", operacaoId, "antes", antes, "depois", depois));
        var resultado = new EstoqueDto.Confirmacao(operacaoId, p.getId(), pedido.versao(), depois);
        movimentos.saveAndFlush(
                new MovimentoEstoque(
                        p,
                        u,
                        operacaoId.toString(),
                        hash,
                        acao,
                        acesso.usuario(),
                        CadastroSupport.motivo(motivo),
                        instanteFisico,
                        mapper.writeValueAsString(antes),
                        mapper.writeValueAsString(resultado)));
        return resultado;
    }

    private Instant agora() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    }
}
