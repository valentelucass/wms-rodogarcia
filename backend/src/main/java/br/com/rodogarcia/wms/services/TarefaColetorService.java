package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.EnderecoDto;
import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.ExpedicaoDto;
import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.dto.TarefaColetorDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.repositories.ConjuntoPosicoesRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.TarefaColetorRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import tools.jackson.databind.json.JsonMapper;

@Service
@Validated
@Transactional(readOnly = true)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class TarefaColetorService {
    private final TarefaColetorRepository tarefas;
    private final AcessoService acesso;
    private final EstoqueService estoque;
    private final MovimentacaoEstoqueService movimentos;
    private final ExpedicaoService expedicao;
    private final InventarioService inventarios;
    private final JsonMapper mapper;
    private final Clock clock;
    private final boolean habilitado;
    private final EnderecoRepository enderecos;
    private final UnidadeLogisticaRepository unidades;
    private final ConjuntoPosicoesRepository conjuntos;

    public TarefaColetorService(
            TarefaColetorRepository tarefas,
            AcessoService acesso,
            EstoqueService estoque,
            MovimentacaoEstoqueService movimentos,
            ExpedicaoService expedicao,
            InventarioService inventarios,
            JsonMapper mapper,
            Clock clock,
            @Value("${wms.coletor-tarefas.enabled:false}") boolean habilitado,
            EnderecoRepository enderecos,
            UnidadeLogisticaRepository unidades,
            ConjuntoPosicoesRepository conjuntos) {
        this.conjuntos = conjuntos;
        this.enderecos = enderecos;
        this.unidades = unidades;
        this.tarefas = tarefas;
        this.acesso = acesso;
        this.estoque = estoque;
        this.movimentos = movimentos;
        this.expedicao = expedicao;
        this.inventarios = inventarios;
        this.mapper = mapper;
        this.clock = clock;
        this.habilitado = habilitado;
    }

    public TarefaColetorDto.Disponibilidade disponibilidade() {
        acesso.usuario();
        return new TarefaColetorDto.Disponibilidade(
                habilitado, habilitado ? null : "As tarefas aguardam ativação neste ambiente.");
    }

    private void ativado() {
        if (!habilitado)
            throw conflito("TAREFAS_NAO_ATIVADAS", "As tarefas aguardam ativação neste ambiente.");
    }

    private void escopo(Long cliente, Long armazem) {
        ativado();
        acesso.usuario();
        if (cliente != null) acesso.cliente(cliente);
        if (armazem != null) acesso.armazem(armazem);
    }

    public TarefaColetorDto.Totais totais(Long clienteId, Long armazemId) {
        escopo(clienteId, armazemId);
        return tarefas.totais(
                clienteId, armazemId, acesso.gestor(), acesso.clientes(), acesso.armazens());
    }

    public PaginaResponse<TarefaColetorDto.Tarefa> listar(
            String tipo, Long clienteId, Long armazemId, int pagina, int tamanho) {
        escopo(clienteId, armazemId);
        CadastroSupport.pagina(pagina, tamanho);
        if (!Set.of("ENDERECAMENTO", "SEPARACAO", "INVENTARIO").contains(tipo))
            throw CadastroSupport.invalido("Tipo de tarefa inválido.");
        return tarefas.listar(
                tipo,
                clienteId,
                armazemId,
                acesso.gestor(),
                acesso.clientes(),
                acesso.armazens(),
                acesso.usuario(),
                pagina,
                tamanho);
    }

    private TarefaColetorDto.Tarefa obter(Long id, boolean bloquear) {
        ativado();
        String usuario = acesso.usuario();
        var t = tarefas.consultar(id, usuario, bloquear);
        if (t == null) throw RegraNegocioException.naoEncontrado();
        acesso.cliente(t.clienteId());
        acesso.armazem(t.armazemId());
        return t;
    }

    public TarefaColetorDto.Detalhe consultar(Long id) {
        return detalhe(obter(id, false));
    }

    public TarefaColetorDto.Tarefa porPedido(Long pedidoId) {
        ativado();
        // A consulta do pedido revalida o acesso antes de procurar sua tarefa, inclusive no
        // histórico.
        expedicao.consultar(pedidoId);
        var tarefa = tarefas.porPedido(pedidoId, acesso.usuario());
        if (tarefa == null) throw RegraNegocioException.naoEncontrado();
        acesso.cliente(tarefa.clienteId());
        acesso.armazem(tarefa.armazemId());
        return tarefa;
    }

    public EnderecoDto.Resposta posicao(Long id, String codigo) {
        var t = obter(id, false);
        if (codigo == null || codigo.isBlank() || codigo.length() > 40)
            throw CadastroSupport.invalido("Leia o código da posição.");
        return EnderecoDto.Resposta.de(
                enderecos
                        .findByArmazemIdAndCodigo(t.armazemId(), codigo.trim())
                        .orElseThrow(RegraNegocioException::naoEncontrado));
    }

    public TarefaColetorDto.Destinos destinos(Long id, String codigo, String segundoCodigo) {
        var a = posicao(id, codigo);
        if (segundoCodigo == null || segundoCodigo.isBlank())
            return new TarefaColetorDto.Destinos(
                    List.of(new EstoqueDto.Destino(a.id(), a.codigo())), null);
        var b = posicao(id, segundoCodigo);
        if (a.id().equals(b.id())) throw CadastroSupport.invalido("Leia duas posições diferentes.");
        var conjunto =
                conjuntos
                        .buscarAtivoPorPar(a.id(), b.id())
                        .orElseThrow(
                                () ->
                                        CadastroSupport.invalido(
                                                "As duas posições precisam formar um conjunto ativo cadastrado."));
        return new TarefaColetorDto.Destinos(
                List.of(
                        new EstoqueDto.Destino(a.id(), a.codigo()),
                        new EstoqueDto.Destino(b.id(), b.codigo())),
                conjunto.getId());
    }

    public TarefaColetorDto.Unidade unidade(Long id, UUID codigo) {
        var t = obter(id, false);
        if ("ENDERECAMENTO".equals(t.tipo())) {
            if (!codigo.toString().equals(t.codigoUnidade()))
                throw RegraNegocioException.naoEncontrado();
        } else if ("SEPARACAO".equals(t.tipo())) {
            if (expedicao.consultar(t.origemId()).pedido().reservas().stream()
                    .noneMatch(r -> codigo.toString().equals(r.codigoUnidade())))
                throw RegraNegocioException.naoEncontrado();
        } else
            throw CadastroSupport.invalido(
                    "O inventário não revela a base esperada durante a contagem.");
        var u =
                unidades.findByCodigo(codigo.toString())
                        .orElseThrow(RegraNegocioException::naoEncontrado);
        return new TarefaColetorDto.Unidade(estoque.consultar(codigo), u.getRevisaoConteudo());
    }

    private TarefaColetorDto.Detalhe detalhe(TarefaColetorDto.Tarefa t) {
        return switch (t.tipo()) {
            case "ENDERECAMENTO" ->
                    new TarefaColetorDto.Detalhe(
                            t,
                            estoque.consultar(UUID.fromString(t.codigoUnidade())),
                            null,
                            List.of(),
                            null);
            case "SEPARACAO" -> {
                var e = expedicao.consultar(t.origemId());
                yield new TarefaColetorDto.Detalhe(t, null, e.pedido(), e.separacoes(), null);
            }
            case "INVENTARIO" ->
                    new TarefaColetorDto.Detalhe(
                            t,
                            null,
                            null,
                            List.of(),
                            inventarios.consultar(t.origemId()).inventario());
            default -> throw CadastroSupport.invalido("Tipo de tarefa inválido.");
        };
    }

    private static RegraNegocioException conflito(String codigo, String mensagem) {
        return RegraNegocioException.conflito(codigo, mensagem);
    }

    private static void naoEncerrada(TarefaColetorDto.Tarefa t) {
        if (Set.of("CONCLUIDA", "CANCELADA").contains(t.situacao()))
            throw conflito("TAREFA_ENCERRADA", "A tarefa já foi encerrada. Atualize a lista.");
    }

    private static void aberta(TarefaColetorDto.Tarefa t) {
        naoEncerrada(t);
        if ("AGUARDANDO_RESERVA".equals(t.situacao()))
            throw conflito(
                    "RESERVA_NECESSARIA",
                    "Confirme a reserva do pedido antes de iniciar a separação.");
    }

    private static void tipo(TarefaColetorDto.Tarefa t, String tipo) {
        if (!tipo.equals(t.tipo()))
            throw CadastroSupport.invalido("A operação não pertence a esta tarefa.");
    }

    private <T> T comando(
            Long id,
            UUID operacaoId,
            String acao,
            Object corpo,
            Class<T> resposta,
            boolean dono,
            Function<TarefaColetorDto.Tarefa, T> executar) {
        var t = obter(id, true);
        String usuario = acesso.usuario();
        String hash =
                NfeXmlService.hash(acao + ":" + usuario + ":" + mapper.writeValueAsString(corpo));
        var anterior = tarefas.operacao(id, operacaoId);
        if (anterior != null) {
            if (!anterior.hash().equals(hash) || !anterior.usuario().equals(usuario))
                throw conflito(
                        "OPERACAO_REUTILIZADA",
                        "Identificador já usado com outro conteúdo ou operador.");
            return mapper.readValue(anterior.resultado(), resposta);
        }
        if (dono && !usuario.equals(t.responsavel()))
            throw conflito(
                    "TAREFA_OUTRO_OPERADOR",
                    "Inicie esta tarefa antes de registrar a operação; ela pode estar com outro operador.");
        T resultado = executar.apply(t);
        tarefas.registrarOperacao(
                id,
                operacaoId,
                acao,
                usuario,
                clock.instant().truncatedTo(ChronoUnit.MICROS),
                hash,
                mapper.writeValueAsString(resultado));
        return resultado;
    }

    @Transactional
    public TarefaColetorDto.Detalhe iniciar(Long id, @NotNull @Valid TarefaColetorDto.Comando d) {
        return comando(
                id,
                d.operacaoId(),
                "INICIAR",
                d,
                TarefaColetorDto.Detalhe.class,
                false,
                t -> {
                    aberta(t);
                    if (t.versao() != d.versao())
                        throw conflito(
                                "VERSAO_DESATUALIZADA", "Tarefa alterada. Atualize a lista.");
                    if (t.responsavel() != null && !t.minha())
                        throw conflito(
                                "TAREFA_OUTRO_OPERADOR", "Outro operador já iniciou esta tarefa.");
                    if (t.responsavel() == null)
                        tarefas.assumir(
                                id,
                                acesso.usuario(),
                                clock.instant().truncatedTo(ChronoUnit.MICROS));
                    return consultar(id);
                });
    }

    @Transactional
    public TarefaColetorDto.Detalhe liberar(Long id, @NotNull @Valid TarefaColetorDto.Comando d) {
        return comando(
                id,
                d.operacaoId(),
                "LIBERAR",
                d,
                TarefaColetorDto.Detalhe.class,
                false,
                t -> {
                    naoEncerrada(t);
                    CadastroSupport.motivo(d.motivo());
                    if (t.versao() != d.versao())
                        throw conflito(
                                "VERSAO_DESATUALIZADA", "Tarefa alterada. Atualize a lista.");
                    if (!t.minha()) acesso.exigirSupervisor();
                    tarefas.liberar(id);
                    return consultar(id);
                });
    }

    @Transactional
    public TarefaColetorDto.Detalhe enderecar(
            Long id, @NotNull @Valid TarefaColetorDto.Enderecar d) {
        return comando(
                id,
                d.operacaoId(),
                "ENDERECAR",
                d,
                TarefaColetorDto.Detalhe.class,
                true,
                t -> {
                    aberta(t);
                    tipo(t, "ENDERECAMENTO");
                    if (!d.codigoLido().toString().equals(t.codigoUnidade()))
                        throw conflito(
                                "ETIQUETA_INVALIDA", "Leia a etiqueta da mercadoria desta tarefa.");
                    if (!d.operacaoId().equals(d.posicionar().operacaoId()))
                        throw CadastroSupport.invalido("Identificadores de operação divergentes.");
                    movimentos.posicionar(d.codigoLido(), d.posicionar());
                    return consultar(id);
                });
    }

    @Transactional
    public TarefaColetorDto.Detalhe separar(Long id, @NotNull @Valid TarefaColetorDto.Separar d) {
        return comando(
                id,
                d.operacaoId(),
                "SEPARAR",
                d,
                TarefaColetorDto.Detalhe.class,
                true,
                t -> {
                    aberta(t);
                    tipo(t, "SEPARACAO");
                    if (!d.operacaoId().equals(d.leitura().operacaoId())
                            || !d.leitura().reservaId().equals(d.destinacao().reservaId()))
                        throw CadastroSupport.invalido(
                                "Leitura e destino devem pertencer à mesma reserva/operação.");
                    if (d.leitura().resolverPendentes())
                        throw CadastroSupport.invalido(
                                "Resolva impedimentos pelo fluxo do supervisor.");
                    var l = expedicao.ler(t.origemId(), d.leitura());
                    UUID separarId =
                            UUID.nameUUIDFromBytes(
                                    (d.operacaoId() + ":separar").getBytes(StandardCharsets.UTF_8));
                    expedicao.separar(
                            t.origemId(),
                            new ExpedicaoDto.Separar(
                                    separarId,
                                    l.expedicao().pedido().versao(),
                                    d.destinacao(),
                                    d.leitura().motivo(),
                                    false));
                    return consultar(id);
                });
    }

    @Transactional
    public InventarioDto.Registro contar(Long id, @NotNull @Valid InventarioDto.Ler d) {
        return comando(
                id,
                d.operacaoId(),
                "CONTAR",
                d,
                InventarioDto.Registro.class,
                true,
                t -> {
                    aberta(t);
                    tipo(t, "INVENTARIO");
                    return inventarios.ler(t.origemId(), d);
                });
    }

    @Transactional
    public InventarioDto.Relatorio finalizar(Long id, @NotNull @Valid InventarioDto.Encerrar d) {
        return comando(
                id,
                d.operacaoId(),
                "FINALIZAR",
                d,
                InventarioDto.Relatorio.class,
                true,
                t -> {
                    aberta(t);
                    tipo(t, "INVENTARIO");
                    return inventarios.finalizar(t.origemId(), d);
                });
    }

    @Transactional
    public InventarioDto.Resumo cancelarInventario(
            Long id, @NotNull @Valid InventarioDto.Encerrar d) {
        return comando(
                id,
                d.operacaoId(),
                "CANCELAR_INVENTARIO",
                d,
                InventarioDto.Resumo.class,
                true,
                t -> {
                    aberta(t);
                    tipo(t, "INVENTARIO");
                    return inventarios.cancelar(t.origemId(), d);
                });
    }
}
