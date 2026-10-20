package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Endereco;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoEndereco;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.VisaoOperacaoRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.IndicadorEstoqueService;
import br.com.rodogarcia.wms.services.PrevisaoFaturamentoService;
import br.com.rodogarcia.wms.services.VisaoOperacaoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisaoOperacaoServiceTest {
    @Test
    void previaIndependenteNaoConsultaMapaNemIndicadoresFisicos() {
        var repo = mock(VisaoOperacaoRepository.class);
        var clientes = mock(ClienteRepository.class);
        var armazens = mock(ArmazemRepository.class);
        var acesso = mock(AcessoService.class);
        var valores = mock(IndicadorEstoqueService.class);
        var previsoes = mock(PrevisaoFaturamentoService.class);
        when(clientes.existsById(1L)).thenReturn(true);
        when(armazens.existsById(2L)).thenReturn(true);
        var agora = Instant.parse("2026-10-20T15:00:00Z");
        var service =
                new VisaoOperacaoService(
                        repo,
                        clientes,
                        armazens,
                        acesso,
                        valores,
                        previsoes,
                        Clock.fixed(agora, ZoneOffset.UTC));
        service.preverFaturamento(1L, 2L, "America/Sao_Paulo");
        verify(acesso).exigirSupervisor();
        verify(acesso).cliente(1L);
        verify(acesso).armazem(2L);
        verify(previsoes)
                .consultar(
                        List.of(1L), List.of(2L), agora, java.time.ZoneId.of("America/Sao_Paulo"));
        org.mockito.Mockito.verifyNoInteractions(repo, valores);
    }

    @Test
    void duasPosicoesDeOitentaArredondamTresPorcentoSemDuplicarUnidadeOuFabricarValor() {
        var repo = mock(VisaoOperacaoRepository.class);
        var clientes = mock(ClienteRepository.class);
        var armazens = mock(ArmazemRepository.class);
        var acesso = mock(AcessoService.class);
        var valores = mock(IndicadorEstoqueService.class);
        var previsoes = mock(PrevisaoFaturamentoService.class);
        when(clientes.existsById(1L)).thenReturn(true);
        when(armazens.existsById(2L)).thenReturn(true);
        when(acesso.supervisor()).thenReturn(true);
        when(repo.capacidade(List.of(2L))).thenReturn(80L);
        when(repo.ocupadas(List.of(2L))).thenReturn(2L);
        when(repo.livres(List.of(2L))).thenReturn(78L);
        when(repo.unidades(List.of(1L), List.of(2L))).thenReturn(1L);
        when(repo.mapa(anyList(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(valores.valoracaoArmazenada(anyList(), anyList(), any(), any()))
                .thenReturn(
                        new br.com.rodogarcia.wms.dto.VisaoOperacaoDto.ValorArmazenado(
                                BigDecimal.ZERO, null, 0, 1, 0, 0, List.of()));
        var service =
                new VisaoOperacaoService(
                        repo,
                        clientes,
                        armazens,
                        acesso,
                        valores,
                        previsoes,
                        Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC));
        var d = service.consultar(1L, 2L, "America/Sao_Paulo", "X", "OCUPADO", 0, 100);
        assertThat(d.ocupacao()).isEqualByComparingTo("3");
        assertThat(d.unidadesArmazenadas()).isEqualTo(1);
        assertThat(d.valorArmazenado()).isNull();
        assertThat(d.valorCompleto()).isFalse();
        when(valores.valoracaoArmazenada(anyList(), anyList(), any(), any()))
                .thenReturn(
                        new br.com.rodogarcia.wms.dto.VisaoOperacaoDto.ValorArmazenado(
                                new BigDecimal("12500.25"),
                                new BigDecimal("12500.25"),
                                1,
                                0,
                                0,
                                0,
                                List.of()));
        when(repo.faturamento(anyList(), anyList(), any(), any()))
                .thenReturn(new BigDecimal("2345.00"));
        d = service.consultar(1L, 2L, "America/Sao_Paulo", "", "TODAS", 0, 100);
        assertThat(d.valorArmazenado()).isEqualByComparingTo("12500.25");
        assertThat(d.valoracao().conhecido()).isEqualByComparingTo("12500.25");
        assertThat(d.faturamentoMes()).isEqualByComparingTo("2345.00");
        verify(repo, times(2)).capacidade(List.of(2L));
        verify(repo, times(2))
                .faturamento(
                        List.of(1L),
                        List.of(2L),
                        Instant.parse("2026-10-01T03:00:00Z"),
                        Instant.parse("2026-11-01T03:00:00Z"));
        org.mockito.Mockito.verifyNoInteractions(previsoes);
    }

    private record ValorFixture(
            IndicadorEstoqueService service,
            br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository unidades,
            br.com.rodogarcia.wms.repositories.EntradaConferidaRepository entradas,
            br.com.rodogarcia.wms.services.CalculoCobrancaService calculos,
            AcessoService acesso) {}

    private ValorFixture valorFixture() {
        var unidades = mock(br.com.rodogarcia.wms.repositories.UnidadeLogisticaRepository.class);
        var entradas = mock(br.com.rodogarcia.wms.repositories.EntradaConferidaRepository.class);
        var calculos = mock(br.com.rodogarcia.wms.services.CalculoCobrancaService.class);
        var acesso = mock(AcessoService.class);
        var service =
                new IndicadorEstoqueService(
                        mock(br.com.rodogarcia.wms.repositories.ProdutoRepository.class),
                        mock(ClienteRepository.class),
                        mock(ArmazemRepository.class),
                        mock(
                                br.com.rodogarcia.wms.repositories
                                        .ConfiguracaoAvisoValidadeRepository.class),
                        entradas,
                        unidades,
                        mock(br.com.rodogarcia.wms.services.EstoqueService.class),
                        calculos,
                        mock(br.com.rodogarcia.wms.services.OperacaoAdministrativaService.class),
                        mock(br.com.rodogarcia.wms.services.AuditoriaService.class),
                        acesso,
                        mock(jakarta.persistence.EntityManager.class),
                        Clock.systemUTC());
        return new ValorFixture(service, unidades, entradas, calculos, acesso);
    }

    @Test
    void valorConhecidoNaoDesapareceComOutraUnidadeSemOrigemENaoDuplicaMotivoDaMesmaUnidade() {
        var f = valorFixture();
        var u1 = mock(UnidadeLogistica.class);
        var u2 = mock(UnidadeLogistica.class);
        var conhecida = mock(br.com.rodogarcia.wms.dto.CalculoCobrancaDto.Contribuicao.class);
        var pendente = mock(br.com.rodogarcia.wms.dto.CalculoCobrancaDto.Contribuicao.class);
        when(conhecida.valorEstoque()).thenReturn(new BigDecimal("125.25"));
        when(f.unidades.ativasParaIndicador(List.of(1L), List.of(2L))).thenReturn(List.of(u1, u2));
        var motivo =
                new br.com.rodogarcia.wms.dto.CalculoCobrancaDto.Pendencia(
                        "VALOR_NOTA_AUSENTE", null, 2L, null, "Valor de origem ausente.");
        when(f.calculos.indicadoresUnidades(anyList(), any(), any()))
                .thenReturn(
                        List.of(
                                new br.com.rodogarcia.wms.services.CalculoCobrancaService
                                        .ValorIndicador(conhecida, List.of()),
                                new br.com.rodogarcia.wms.services.CalculoCobrancaService
                                        .ValorIndicador(pendente, List.of(motivo, motivo))));
        var v =
                f.service.valoracaoArmazenada(
                        List.of(1L), List.of(2L), Instant.now(), ZoneOffset.UTC);
        assertThat(v.conhecido()).isEqualByComparingTo("125.25");
        assertThat(v.total()).isNull();
        assertThat(v.unidadesValoradas()).isEqualTo(1);
        assertThat(v.unidadesPendentes()).isEqualTo(1);
        assertThat(v.pendencias())
                .singleElement()
                .satisfies(
                        p -> {
                            assertThat(p.codigo()).isEqualTo("VALOR_NOTA_AUSENTE");
                            assertThat(p.ocorrencias()).isEqualTo(1);
                        });
        verify(f.calculos)
                .indicadoresUnidades(
                        org.mockito.ArgumentMatchers.eq(List.of(u1, u2)), any(), any());
        verify(f.acesso).exigirSupervisor();
        verify(f.acesso).cliente(1L);
        verify(f.acesso).armazem(2L);
    }

    @Test
    void valorDaEntradaUsaSomenteMercadoriaBoaSemDuplicarAvariadaAntesDaUnitizacao() {
        var f = valorFixture();
        var item = mock(br.com.rodogarcia.wms.models.ItemNotaEntrada.class);
        var chegada = mock(br.com.rodogarcia.wms.models.ItemChegada.class);
        when(chegada.getItemNota()).thenReturn(item);
        when(item.getValorMercadoria()).thenReturn(new BigDecimal("200"));
        when(item.getQuantidadePrevista()).thenReturn(new BigDecimal("10"));
        var e =
                new br.com.rodogarcia.wms.models.EntradaConferida(
                        chegada,
                        Instant.now(),
                        Instant.now(),
                        new BigDecimal("6"),
                        new BigDecimal("4"));
        when(f.entradas.pendentesParaIndicador(anyList(), anyList())).thenReturn(List.of(e));
        var v =
                f.service.valoracaoArmazenada(
                        List.of(1L), List.of(2L), Instant.now(), ZoneOffset.UTC);
        assertThat(v.total()).isEqualByComparingTo("120");
        assertThat(v.entradasValoradas()).isEqualTo(1);
        assertThat(v.entradasPendentes()).isZero();
    }

    @Test
    void entradaSomenteAvariadaValeZeroMesmoSemPrecoSemFabricarPendencia() {
        var f = valorFixture();
        var e =
                new br.com.rodogarcia.wms.models.EntradaConferida(
                        mock(br.com.rodogarcia.wms.models.ItemChegada.class),
                        Instant.now(),
                        Instant.now(),
                        BigDecimal.ZERO,
                        new BigDecimal("10"));
        when(f.entradas.pendentesParaIndicador(anyList(), anyList())).thenReturn(List.of(e));
        var v =
                f.service.valoracaoArmazenada(
                        List.of(1L), List.of(2L), Instant.now(), ZoneOffset.UTC);
        assertThat(v.total()).isEqualByComparingTo("0");
        assertThat(v.entradasValoradas()).isEqualTo(1);
        assertThat(v.pendencias()).isEmpty();
    }

    @Test
    void entradaBoaSemValorDeOrigemTemConhecidoZeroSemApresentarTotalZero() {
        var f = valorFixture();
        var item = mock(br.com.rodogarcia.wms.models.ItemNotaEntrada.class);
        var chegada = mock(br.com.rodogarcia.wms.models.ItemChegada.class);
        when(chegada.getItemNota()).thenReturn(item);
        when(item.getQuantidadePrevista()).thenReturn(new BigDecimal("10"));
        var e =
                new br.com.rodogarcia.wms.models.EntradaConferida(
                        chegada, Instant.now(), Instant.now(), BigDecimal.ONE, BigDecimal.ZERO);
        when(f.entradas.pendentesParaIndicador(anyList(), anyList())).thenReturn(List.of(e));
        var v =
                f.service.valoracaoArmazenada(
                        List.of(1L), List.of(2L), Instant.now(), ZoneOffset.UTC);
        assertThat(v.conhecido()).isEqualByComparingTo("0");
        assertThat(v.total()).isNull();
        assertThat(v.entradasValoradas()).isZero();
        assertThat(v.entradasPendentes()).isEqualTo(1);
        assertThat(v.pendencias())
                .singleElement()
                .satisfies(p -> assertThat(p.codigo()).isEqualTo("VALOR_NOTA_AUSENTE"));
    }

    @Test
    void valoracaoRecusaAcessoAntesDeConsultarEstoque() {
        var f = valorFixture();
        org.mockito.Mockito.doThrow(new SecurityException("Sem acesso"))
                .when(f.acesso)
                .exigirSupervisor();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                f.service.valoracaoArmazenada(
                                        List.of(1L), List.of(2L), Instant.now(), ZoneOffset.UTC))
                .isInstanceOf(SecurityException.class);
        org.mockito.Mockito.verifyNoInteractions(f.unidades, f.entradas, f.calculos);
    }

    @Test
    void ocupacaoDeOutroClienteNaoExpoeProdutoUnidadeReservaOuBloqueio() {
        var repo = mock(VisaoOperacaoRepository.class);
        var acesso = mock(AcessoService.class);
        var e = mock(Endereco.class);
        var a = mock(Armazem.class);
        var u = mock(UnidadeLogistica.class);
        var p = mock(PedidoEntrada.class);
        var c = mock(Cliente.class);
        when(e.getArmazem()).thenReturn(a);
        when(e.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);
        when(e.getTipo()).thenReturn(TipoEndereco.ARMAZENAGEM);
        when(a.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);
        when(a.getId()).thenReturn(3L);
        when(u.getPedido()).thenReturn(p);
        when(p.getCliente()).thenReturn(c);
        when(c.getId()).thenReturn(1L);
        when(u.getId()).thenReturn(4L);
        when(u.isBloqueada()).thenReturn(true);
        when(acesso.clientes()).thenReturn(List.of(2L));
        when(repo.detalhe(10L)).thenReturn(new VisaoOperacaoRepository.Celula(e, u));
        var service =
                new VisaoOperacaoService(
                        repo,
                        mock(ClienteRepository.class),
                        mock(ArmazemRepository.class),
                        acesso,
                        mock(IndicadorEstoqueService.class),
                        mock(PrevisaoFaturamentoService.class),
                        Clock.systemUTC());
        var d = service.detalhe(10L);
        assertThat(d.conteudoRestrito()).isTrue();
        assertThat(d.unidades()).isEmpty();
        assertThat(d.endereco().ocupada()).isTrue();
        assertThat(d.endereco().disponivel()).isFalse();
        assertThat(d.endereco().bloqueada()).isFalse();
        verify(repo, never()).reservada(4L);
    }
}
