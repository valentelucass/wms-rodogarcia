package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.InventarioDto;
import br.com.rodogarcia.wms.exceptions.RegraNegocioException;
import br.com.rodogarcia.wms.models.InventarioItem;
import br.com.rodogarcia.wms.models.InventarioSessao;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoInventario;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.EnderecoRepository;
import br.com.rodogarcia.wms.repositories.InventarioRepository;
import br.com.rodogarcia.wms.repositories.OcupacaoEnderecoRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

/** Fronteiras do serviço com doubles; sem Spring/DataSource/conexão. */
class InventarioServiceTest {
    final InventarioRepository repository = mock(InventarioRepository.class);
    final UnidadeLogisticaRepository units = mock(UnidadeLogisticaRepository.class);
    final AcessoService access = mock(AcessoService.class);
    final OperacaoAdministrativaService operations = mock(OperacaoAdministrativaService.class);
    final AuditoriaService audit = mock(AuditoriaService.class);
    final Clock clock = Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), ZoneOffset.UTC);

    private void scopedSession(TipoInventario tipo, Long produto, Long endereco) {
        var i =
                new InventarioSessao(
                        901L,
                        2,
                        83L,
                        46L,
                        tipo,
                        produto,
                        endereco,
                        "EM_ANDAMENTO",
                        clock.instant(),
                        null,
                        "operador",
                        "Conferência física",
                        0,
                        null);
        when(repository.consultar(901L, false)).thenReturn(i);
        when(repository.consultar(901L, true)).thenReturn(i);
    }

    private void unit(UUID codigo, long produtoId) {
        var scope = mock(UnidadeLogisticaRepository.EscopoUnidade.class);
        when(scope.getClienteId()).thenReturn(83L);
        when(scope.getArmazemId()).thenReturn(46L);
        when(units.buscarEscopoPorCodigo(codigo.toString())).thenReturn(Optional.of(scope));
        var u = mock(UnidadeLogistica.class);
        var p = mock(Produto.class);
        when(p.getId()).thenReturn(produtoId);
        when(p.getPrecisaoQuantidade()).thenReturn(6);
        when(u.getProduto()).thenReturn(p);
        when(units.findByCodigo(codigo.toString())).thenReturn(Optional.of(u));
    }

    @Test
    void skuForaDaSelecaoNaoEhRegistrado() {
        scopedSession(TipoInventario.SKU, 17L, null);
        var codigo = UUID.randomUUID();
        unit(codigo, 18L);
        var d =
                new InventarioDto.Ler(
                        UUID.randomUUID(), 2L, codigo, java.math.BigDecimal.ONE, null, false);
        assertThatThrownBy(() -> service(true).ler(901L, d))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("SKU selecionado");
        verify(repository, never()).ler(any(), any(), any(), any(), any(), any());
    }

    @Test
    void inventarioDePosicaoExigeConfirmarAPosicaoSelecionada() {
        scopedSession(TipoInventario.POSICAO, null, 71L);
        var codigo = UUID.randomUUID();
        unit(codigo, 17L);
        var d =
                new InventarioDto.Ler(
                        UUID.randomUUID(), 2L, codigo, java.math.BigDecimal.ONE, 72L, false);
        assertThatThrownBy(() -> service(true).ler(901L, d))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("posição selecionada");
        verify(repository, never()).ler(any(), any(), any(), any(), any(), any());
    }

    @Test
    void leituraZeroTambemImpedeSomarNovamenteAMesmaEtiqueta() {
        scopedSession(TipoInventario.CEGAS, null, null);
        var codigo = UUID.randomUUID();
        unit(codigo, 17L);
        var b =
                new InventarioItem.Base(
                        1L,
                        codigo.toString(),
                        1,
                        17L,
                        "SKU-17",
                        "Produto",
                        "UN",
                        6,
                        java.math.BigDecimal.TEN,
                        List.of());
        when(repository.item(901L, codigo.toString()))
                .thenReturn(
                        new InventarioItem(
                                b,
                                true,
                                java.math.BigDecimal.ZERO,
                                null,
                                null,
                                clock.instant(),
                                "operador"));
        var d =
                new InventarioDto.Ler(
                        UUID.randomUUID(), 2L, codigo, java.math.BigDecimal.ONE, null, false);
        assertThatThrownBy(() -> service(true).ler(901L, d))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já contada");
        verify(repository, never()).ler(any(), any(), any(), any(), any(), any());
    }

    InventarioService service(boolean enabled) {
        return new InventarioService(
                repository,
                units,
                mock(OcupacaoEnderecoRepository.class),
                mock(ProdutoRepository.class),
                mock(EnderecoRepository.class),
                mock(ClienteRepository.class),
                mock(ArmazemRepository.class),
                access,
                operations,
                audit,
                JsonMapper.builder().build(),
                clock,
                enabled);
    }

    InventarioSessao session(String situacao) {
        return new InventarioSessao(
                901L,
                2,
                83L,
                46L,
                TipoInventario.CEGAS,
                null,
                null,
                situacao,
                clock.instant(),
                null,
                "operador",
                "Contagem física",
                0,
                null);
    }

    @Test
    void recursoDesativadoNaoConsultaTabelasNovas() {
        var s = service(false);
        assertThat(s.disponibilidade().disponivel()).isFalse();
        assertThatThrownBy(() -> s.listar(83L, 46L, 0, 20))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ativação");
        verifyNoInteractions(repository, units, operations, audit);
    }

    @Test
    void inicioRepetidoRecuperaConfirmacaoOriginalSemAbrirOutroInventario() {
        var original =
                new InventarioDto.Resumo(
                        901L,
                        0,
                        83L,
                        46L,
                        TipoInventario.CEGAS,
                        null,
                        null,
                        "EM_ANDAMENTO",
                        clock.instant(),
                        null,
                        "operador",
                        0,
                        null);
        when(operations.repetida(any(), any(), eq(InventarioDto.Resumo.class)))
                .thenReturn(original);
        var d =
                new InventarioDto.Iniciar(
                        UUID.randomUUID(),
                        83L,
                        46L,
                        TipoInventario.CEGAS,
                        null,
                        null,
                        "Conferência física");
        assertThat(service(true).iniciar(d)).isSameAs(original);
        verify(access).cliente(83L);
        verify(access).armazem(46L);
        verifyNoInteractions(repository, units, audit);
    }

    @Test
    void relatorioDeInventarioAbertoNaoRevelaBase() {
        when(repository.consultar(901L, false)).thenReturn(session("EM_ANDAMENTO"));
        assertThatThrownBy(() -> service(true).relatorio(901L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("após encerrar");
        verify(repository, never()).itens(any());
        verify(repository, never()).relatorio(any());
    }

    @Test
    void contextoNaoAutorizadoNaoRetornaLeiturasOuRelatorio() {
        when(repository.consultar(901L, false)).thenReturn(session("CONCLUIDO"));
        doThrow(new AccessDeniedException("Negado")).when(access).cliente(83L);
        assertThatThrownBy(() -> service(true).relatorio(901L))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).relatorio(any());
        verify(repository, never()).ultimasLeituras(any());
    }

    @Test
    void versaoDiferenteRecusaNovaLeituraAntesDeConsultarEstoque() {
        when(repository.consultar(901L, false)).thenReturn(session("EM_ANDAMENTO"));
        when(repository.consultar(901L, true)).thenReturn(session("EM_ANDAMENTO"));
        var d =
                new InventarioDto.Ler(
                        UUID.randomUUID(),
                        1L,
                        UUID.randomUUID(),
                        java.math.BigDecimal.ONE,
                        null,
                        false);
        assertThatThrownBy(() -> service(true).ler(901L, d))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Outra leitura");
        verifyNoInteractions(units);
        verify(repository, never()).ler(any(), any(), any(), any(), any(), any());
    }

    @Test
    void chaveRepetidaRetornaConfirmacaoOriginalAntesDeVerificarVersao() {
        when(repository.consultar(901L, false)).thenReturn(session("EM_ANDAMENTO"));
        when(repository.consultar(901L, true)).thenReturn(session("EM_ANDAMENTO"));
        var original = new InventarioDto.Registro(null, null);
        when(operations.repetida(any(), any(), eq(InventarioDto.Registro.class)))
                .thenReturn(original);
        var d =
                new InventarioDto.Ler(
                        UUID.randomUUID(),
                        0L,
                        UUID.randomUUID(),
                        java.math.BigDecimal.ONE,
                        null,
                        false);
        assertThat(service(true).ler(901L, d)).isSameAs(original);
        verifyNoInteractions(units);
    }

    @Test
    void encerramentoSemConfirmacaoDasNaoLidasNaoMexeNoEstoque() {
        when(repository.consultar(901L, false)).thenReturn(session("EM_ANDAMENTO"));
        when(repository.consultar(901L, true)).thenReturn(session("EM_ANDAMENTO"));
        var d = new InventarioDto.Encerrar(UUID.randomUUID(), 2L, false, "Encerramento conferido");
        assertThatThrownBy(() -> service(true).finalizar(901L, d))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("faltantes");
        verifyNoInteractions(units, audit);
    }

    @Test
    void consultaAbertaRetornaApenasObservacoesInformadas() {
        when(repository.consultar(901L, false)).thenReturn(session("EM_ANDAMENTO"));
        when(repository.ultimasLeituras(901L)).thenReturn(List.of());
        var d = service(true).consultar(901L);
        assertThat(d.inventario().divergencias()).isNull();
        assertThat(d.leituras()).isEmpty();
        verify(repository, never()).itens(any());
        verify(repository, never()).relatorio(any());
    }

    @Test
    void operadorNaoCancelaInventario() {
        doThrow(new AccessDeniedException("Negado")).when(access).exigirSupervisor();
        var d =
                new InventarioDto.Encerrar(
                        UUID.randomUUID(), 2L, false, "Cancelamento identificado");
        assertThatThrownBy(() -> service(true).cancelar(901L, d))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository, units, audit);
    }
}
