package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.EstoqueDto;
import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.dto.TarefaColetorDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.repositories.ConjuntoPosicoesRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.TarefaColetorRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class TarefaColetorServiceTest {
    private final TarefaColetorRepository repository = mock(TarefaColetorRepository.class);
    private final AcessoService access = mock(AcessoService.class);
    private final EstoqueService stock = mock(EstoqueService.class);
    private final MovimentacaoEstoqueService moves = mock(MovimentacaoEstoqueService.class);
    private final ExpedicaoService shipping = mock(ExpedicaoService.class);
    private final InventarioService inventory = mock(InventarioService.class);
    private final UnidadeLogisticaRepository units = mock(UnidadeLogisticaRepository.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final UUID CODE = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private TarefaColetorService service(boolean active) {
        when(access.usuario()).thenReturn("operador");
        return new TarefaColetorService(
                repository,
                access,
                stock,
                moves,
                shipping,
                inventory,
                mapper,
                Clock.systemUTC(),
                active,
                mock(EnderecoRepository.class),
                units,
                mock(ConjuntoPosicoesRepository.class));
    }

    private TarefaColetorDto.Tarefa task(String type, String status, String owner) {
        return new TarefaColetorDto.Tarefa(
                5L,
                2,
                type,
                91L,
                1L,
                "Cliente",
                2L,
                "Armazém",
                "REF",
                "SKU",
                "Produto",
                CODE.toString(),
                status,
                null,
                Instant.parse("2026-10-28T12:00:00Z"),
                owner,
                null,
                "operador".equals(owner));
    }

    private void returned(TarefaColetorDto.Tarefa task) {
        when(repository.consultar(eq(5L), eq("operador"), anyBoolean())).thenReturn(task);
    }

    private TarefaColetorDto.Comando command() {
        return new TarefaColetorDto.Comando(UUID.randomUUID(), 2L, "Iniciar tarefa no coletor");
    }

    @Test
    void desativadoNaoAcessaTabelasNovas() {
        var s = service(false);
        assertThat(s.disponibilidade().disponivel()).isFalse();
        assertThatThrownBy(() -> s.totais(null, null)).isInstanceOf(RegraNegocioException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void consultaRevalidaClienteAntesDeRevelarEstoque() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "PENDENTE", null));
        doThrow(new AccessDeniedException("negado")).when(access).cliente(1L);
        assertThatThrownBy(() -> s.consultar(5L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(stock, inventory, shipping);
    }

    @Test
    void localizaTarefaDoPedidoInclusiveConcluidaSemCriarNova() {
        var s = service(true);
        var tarefa = task("SEPARACAO", "CONCLUIDA", "operador");
        when(repository.porPedido(91L, "operador")).thenReturn(tarefa);
        assertThat(s.porPedido(91L)).isSameAs(tarefa);
        verify(shipping).consultar(91L);
        verify(access).cliente(1L);
        verify(access).armazem(2L);
        verify(repository, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void pedidoAntigoSemTarefaNaoRecebeCriacaoRetroativa() {
        var s = service(true);
        assertThatThrownBy(() -> s.porPedido(91L)).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).registrar(any(), any(), any(), any(), any());
    }

    @Test
    void pedidoSemAcessoNaoRevelaExistenciaDaTarefa() {
        var s = service(true);
        doThrow(new AccessDeniedException("negado")).when(shipping).consultar(91L);
        assertThatThrownBy(() -> s.porPedido(91L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rascunhoNaoPodeSerIniciado() {
        var s = service(true);
        returned(task("SEPARACAO", "AGUARDANDO_RESERVA", null));
        assertThatThrownBy(() -> s.iniciar(5L, command())).hasMessageContaining("reserva");
        verify(repository, never()).assumir(any(), any(), any());
    }

    @Test
    void reservaDeOutroOperadorNaoPodeSerAssumida() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "EM_ANDAMENTO", "outro"));
        assertThatThrownBy(() -> s.iniciar(5L, command())).hasMessageContaining("Outro operador");
        verify(repository, never()).assumir(any(), any(), any());
    }

    @Test
    void tarefaConcluidaPeloManualNaoReabre() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "CONCLUIDA", null));
        assertThatThrownBy(() -> s.iniciar(5L, command())).hasMessageContaining("encerrada");
    }

    @Test
    void versaoAntigaNaoAlteraResponsavel() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "PENDENTE", null));
        assertThatThrownBy(
                        () ->
                                s.iniciar(
                                        5L,
                                        new TarefaColetorDto.Comando(
                                                UUID.randomUUID(), 1L, "Iniciar tarefa")))
                .hasMessageContaining("alterada");
        verify(repository, never()).assumir(any(), any(), any());
    }

    @Test
    void iniciarPersisteDonoEEventoNaMesmaOperacao() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "PENDENTE", null));
        var c = command();
        s.iniciar(5L, c);
        verify(repository).assumir(eq(5L), eq("operador"), any());
        verify(repository)
                .registrarOperacao(
                        eq(5L),
                        eq(c.operacaoId()),
                        eq("INICIAR"),
                        eq("operador"),
                        any(),
                        any(),
                        any());
    }

    @Test
    void respostaIncertaReutilizaResultadoSemReexecutar() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "EM_ANDAMENTO", "operador"));
        var c = command();
        var answer =
                new TarefaColetorDto.Detalhe(
                        task("ENDERECAMENTO", "EM_ANDAMENTO", "operador"),
                        null,
                        null,
                        List.of(),
                        null);
        String hash = NfeXmlService.hash("INICIAR:operador:" + mapper.writeValueAsString(c));
        when(repository.operacao(5L, c.operacaoId()))
                .thenReturn(
                        new TarefaColetorRepository.Operacao(
                                hash, "operador", mapper.writeValueAsString(answer)));
        assertThat(s.iniciar(5L, c).tarefa().responsavel()).isEqualTo("operador");
        verify(repository, never()).assumir(any(), any(), any());
        verifyNoInteractions(stock, shipping, inventory);
    }

    @Test
    void chaveReutilizadaComOutroConteudoRecusa() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "PENDENTE", null));
        var c = command();
        when(repository.operacao(5L, c.operacaoId()))
                .thenReturn(new TarefaColetorRepository.Operacao("outro", "operador", "{}"));
        assertThatThrownBy(() -> s.iniciar(5L, c)).hasMessageContaining("Identificador");
    }

    @Test
    void unidadeDeOutraTarefaNaoPodeSerMovida() {
        var s = service(true);
        returned(task("ENDERECAMENTO", "EM_ANDAMENTO", "operador"));
        var id = UUID.randomUUID();
        var d =
                new TarefaColetorDto.Enderecar(
                        id,
                        UUID.randomUUID(),
                        new EstoqueDto.Posicionar(
                                id, 0L, null, null, List.of(), "Endereçar tarefa"));
        assertThatThrownBy(() -> s.enderecar(5L, d)).hasMessageContaining("etiqueta");
        verifyNoInteractions(moves);
    }

    @Test
    void inventarioNuncaExibeUnidadeEsperadaPeloAtalho() {
        var s = service(true);
        returned(task("INVENTARIO", "EM_ANDAMENTO", "operador"));
        assertThatThrownBy(() -> s.unidade(5L, CODE)).hasMessageContaining("base esperada");
        verifyNoInteractions(units, stock);
    }

    @Test
    void contagemExigeResponsavelDaTarefa() {
        var s = service(true);
        returned(task("INVENTARIO", "EM_ANDAMENTO", "outro"));
        var d =
                new InventarioDto.Ler(
                        UUID.randomUUID(), 1L, CODE, java.math.BigDecimal.ONE, null, false);
        assertThatThrownBy(() -> s.contar(5L, d)).hasMessageContaining("operador");
        verifyNoInteractions(inventory);
    }

    @Test
    void listaUsaEscoposPermitidosSemPromoverOperadorAGestor() {
        var s = service(true);
        when(access.clientes()).thenReturn(List.of(1L));
        when(access.armazens()).thenReturn(List.of(2L));
        s.listar("INVENTARIO", null, null, 0, 20);
        verify(repository)
                .listar(
                        "INVENTARIO",
                        null,
                        null,
                        false,
                        List.of(1L),
                        List.of(2L),
                        "operador",
                        0,
                        20);
    }

    @Test
    void registroDesativadoNaoTocaBanco() {
        new TarefaColetorRegistro(repository, Clock.systemUTC(), false)
                .registrar("INVENTARIO", 1L, 1L, 2L);
        verifyNoInteractions(repository);
    }

    @Test
    void podeDevolverTarefaQuandoReservaFoiRevertida() {
        var s = service(true);
        returned(task("SEPARACAO", "AGUARDANDO_RESERVA", "operador"));
        when(shipping.consultar(91L))
                .thenReturn(mock(br.com.rodogarcia.wms.dto.ExpedicaoDto.Detalhe.class));
        s.liberar(5L, command());
        verify(repository).liberar(5L);
    }
}
