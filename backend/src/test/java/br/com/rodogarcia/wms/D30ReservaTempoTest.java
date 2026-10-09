package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.ItemPedidoSaida;
import br.com.rodogarcia.wms.models.PedidoSaida;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.ReservaSaida;
import br.com.rodogarcia.wms.models.SituacaoReservaSaida;
import br.com.rodogarcia.wms.models.UnidadeLogistica;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Marcos de reserva com origem/quantidade fixas; não confunde situação com prova de instante. */
class D30ReservaTempoTest {
    @ParameterizedTest
    @EnumSource(
            value = SituacaoReservaSaida.class,
            names = {"CANCELADA", "REVERTIDA", "RETIRADA"})
    void reservaAtivaTemFimNuloEEncerradaConservaOrigemECriacao(SituacaoReservaSaida fim) {
        Instant criada = Instant.parse("2026-09-01T12:00:00.123456Z");
        Instant encerrada = Instant.parse("2026-09-02T12:00:00.234567Z");
        var pedido = new PedidoSaida(mock(Cliente.class), mock(Armazem.class), "D30-TEMPO", criada);
        var item = new ItemPedidoSaida(pedido, mock(Produto.class), new BigDecimal("10"));
        var unidade = mock(UnidadeLogistica.class);
        var reserva =
                new ReservaSaida(
                        item,
                        unidade,
                        "00000000-0000-0000-0000-000000000030",
                        new BigDecimal("4.000000"),
                        criada);
        assertThat(reserva.getSituacao()).isEqualTo(SituacaoReservaSaida.ATIVA);
        assertThat(reserva.getEncerradaEm()).isNull();
        assertThat(reserva.getCriadaEm()).isEqualTo(criada);
        reserva.encerrar(fim, encerrada);
        assertThat(reserva.getSituacao()).isEqualTo(fim);
        assertThat(reserva.getEncerradaEm()).isEqualTo(encerrada);
        assertThat(reserva.getCriadaEm()).isEqualTo(criada);
        assertThat(reserva.getQuantidade()).isEqualTo(new BigDecimal("4.000000"));
        assertThat(reserva.getItem()).isSameAs(item);
        assertThat(reserva.getUnidade()).isSameAs(unidade);
    }
}
