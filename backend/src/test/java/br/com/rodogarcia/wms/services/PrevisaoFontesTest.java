package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.FatoServicoDto;
import br.com.rodogarcia.wms.models.BaixaSaida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.RetiradaSaida;
import br.com.rodogarcia.wms.models.ServicoCobranca;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import br.com.rodogarcia.wms.repositories.BaixaSaidaRepository;
import br.com.rodogarcia.wms.repositories.RetiradaSaidaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PrevisaoFontesTest {
    private static final Instant AGORA = Instant.parse("2026-10-20T15:00:00Z");
    private final RetiradaSaidaRepository retiradas = mock(RetiradaSaidaRepository.class);
    private final BaixaSaidaRepository baixas = mock(BaixaSaidaRepository.class);
    private final FatoServicoService fatos = mock(FatoServicoService.class);
    private final FontesPrevisaoServicoService fontes =
            new FontesPrevisaoServicoService(retiradas, baixas, fatos);

    @Test
    void entradaExigeMarcoFisicoAteReferenciaSemUsarFuturoOuSomenteUnidadeCriada() {
        var real = mock(UnidadeLogistica.class);
        var futura = mock(UnidadeLogistica.class);
        var semMarco = mock(UnidadeLogistica.class);
        when(real.getPrimeiroEnderecamentoEm()).thenReturn(AGORA.minusSeconds(60));
        when(futura.getPrimeiroEnderecamentoEm()).thenReturn(AGORA.plusSeconds(60));
        var eventos =
                fontes.eventos(
                        1L,
                        2L,
                        LocalDate.parse("2026-10-20"),
                        LocalDate.parse("2026-10-21"),
                        ZoneId.of("America/Sao_Paulo"),
                        AGORA,
                        List.of(real, futura, semMarco));
        assertThat(eventos)
                .singleElement()
                .satisfies(
                        e -> {
                            assertThat(e.tipo()).isEqualTo("ENTRADA");
                            assertThat(e.unidade()).isSameAs(real);
                        });
        verify(retiradas)
                .paraPrevisao(
                        1L,
                        2L,
                        Instant.parse("2026-10-20T03:00:00Z"),
                        Instant.parse("2026-10-21T03:00:00Z"),
                        AGORA);
        verifyNoInteractions(fatos);
    }

    @Test
    void saidaPorProdutoUsaProdutosDasBaixasEConservaChaveEstavel() {
        var pedido = mock(PedidoSaida.class);
        when(pedido.getId()).thenReturn(5L);
        var retirada = mock(RetiradaSaida.class);
        when(retirada.getId()).thenReturn(6L);
        when(retirada.getPedido()).thenReturn(pedido);
        when(retirada.getRetiradaEm()).thenReturn(AGORA.minusSeconds(60));
        var produto = mock(Produto.class);
        when(produto.getId()).thenReturn(7L);
        var unidade = mock(UnidadeLogistica.class);
        when(unidade.getProduto()).thenReturn(produto);
        when(unidade.getTipo()).thenReturn(TipoUnidadeLogistica.BOBINA);
        var reserva = mock(ReservaSaida.class);
        when(reserva.getUnidade()).thenReturn(unidade);
        var baixa = mock(BaixaSaida.class);
        when(baixa.getReserva()).thenReturn(reserva);
        var evento =
                new FontesPrevisaoServicoService.Evento(
                        "SAIDA", retirada.getRetiradaEm(), null, retirada, List.of(baixa, baixa));
        var servico = mock(ServicoCobranca.class);
        when(servico.getId()).thenReturn(8L);
        when(servico.getUnidade()).thenReturn("QUANTIDADE_PRODUTO");
        assertThat(evento.produtos(servico)).containsExactly(7L);
        assertThat(evento.chave(7L)).isEqualTo("SAIDA:RETIRADA:6:SKU:7");
        fontes.comprovar(1L, 2L, evento, servico, 7L);
        var comando = ArgumentCaptor.forClass(FatoServicoDto.Registrar.class);
        verify(fatos).projetarExecucao(eq(servico), comando.capture());
        assertThat(comando.getValue().pedidoSaidaId()).isEqualTo(5L);
        assertThat(comando.getValue().produtoId()).isEqualTo(7L);
        assertThat(comando.getValue().quantidade()).isNull();
        assertThat(comando.getValue().referenciaExecucao()).isNull();
        org.mockito.Mockito.verify(fatos, org.mockito.Mockito.never()).registrar(any());
    }
}
