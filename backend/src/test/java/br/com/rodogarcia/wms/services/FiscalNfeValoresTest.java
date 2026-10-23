package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FiscalNfeValoresTest {
    @Test
    void tresRetiradasConservamOValorInclusiveUltimoCentavo() {
        var valor = new BigDecimal("100.00");
        var q = new BigDecimal("3");
        assertThat(FiscalNfeValores.parcela(valor, q, BigDecimal.ZERO, BigDecimal.ONE))
                .isEqualByComparingTo("33.33");
        assertThat(FiscalNfeValores.parcela(valor, q, BigDecimal.ONE, BigDecimal.ONE))
                .isEqualByComparingTo("33.34");
        assertThat(FiscalNfeValores.parcela(valor, q, new BigDecimal("2"), BigDecimal.ONE))
                .isEqualByComparingTo("33.33");
    }

    @Test
    void fracionamentoDePalletUsaAParcelaDaOrigem() {
        assertThat(
                        FiscalNfeValores.parcela(
                                new BigDecimal("22400"),
                                new BigDecimal("32000"),
                                new BigDecimal("1000"),
                                new BigDecimal("127.123456")))
                .isEqualByComparingTo("88.99");
    }

    @Test
    void origemSemValorOuQuantidadeExcedidaNaoGeraPrecoInventado() {
        assertThatThrownBy(
                        () ->
                                FiscalNfeValores.parcela(
                                        null, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                FiscalNfeValores.parcela(
                                        BigDecimal.TEN,
                                        BigDecimal.TEN,
                                        BigDecimal.TEN,
                                        BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(
                        () ->
                                FiscalNfeValores.parcela(
                                        BigDecimal.TEN,
                                        BigDecimal.ZERO,
                                        BigDecimal.ZERO,
                                        BigDecimal.ONE))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void zeroConhecidoNaoEhValorAusente() {
        assertThat(
                        FiscalNfeValores.parcela(
                                BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ONE))
                .isEqualByComparingTo("0.00");
    }
}
