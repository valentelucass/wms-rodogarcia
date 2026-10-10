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
import br.com.rodogarcia.wms.services.VisaoOperacaoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class VisaoOperacaoServiceTest {
    @Test
    void duasPosicoesDeOitentaArredondamTresPorcentoSemDuplicarUnidadeOuFabricarValor() {
        var repo = mock(VisaoOperacaoRepository.class);
        var clientes = mock(ClienteRepository.class);
        var armazens = mock(ArmazemRepository.class);
        var acesso = mock(AcessoService.class);
        var valores = mock(IndicadorEstoqueService.class);
        when(clientes.existsById(1L)).thenReturn(true);
        when(armazens.existsById(2L)).thenReturn(true);
        when(acesso.supervisor()).thenReturn(true);
        when(repo.capacidade(List.of(2L))).thenReturn(80L);
        when(repo.ocupadas(List.of(2L))).thenReturn(2L);
        when(repo.livres(List.of(2L))).thenReturn(78L);
        when(repo.unidades(List.of(1L), List.of(2L))).thenReturn(1L);
        when(repo.mapa(anyList(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(valores.valorArmazenado(anyList(), anyList(), any(), any())).thenReturn(null);
        var service =
                new VisaoOperacaoService(
                        repo,
                        clientes,
                        armazens,
                        acesso,
                        valores,
                        Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC));
        var d = service.consultar(1L, 2L, "America/Sao_Paulo", "X", "OCUPADO", 0, 100);
        assertThat(d.ocupacao()).isEqualByComparingTo("3");
        assertThat(d.unidadesArmazenadas()).isEqualTo(1);
        assertThat(d.valorArmazenado()).isNull();
        assertThat(d.valorCompleto()).isFalse();
        when(valores.valorArmazenado(anyList(), anyList(), any(), any()))
                .thenReturn(new BigDecimal("12500.25"));
        when(repo.faturamento(anyList(), anyList(), any(), any()))
                .thenReturn(new BigDecimal("2345.00"));
        d = service.consultar(1L, 2L, "America/Sao_Paulo", "", "TODAS", 0, 100);
        assertThat(d.valorArmazenado()).isEqualByComparingTo("12500.25");
        assertThat(d.faturamentoMes()).isEqualByComparingTo("2345.00");
        verify(repo, times(2)).capacidade(List.of(2L));
        verify(repo, times(2))
                .faturamento(
                        List.of(1L),
                        List.of(2L),
                        Instant.parse("2026-10-01T03:00:00Z"),
                        Instant.parse("2026-11-01T03:00:00Z"));
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
