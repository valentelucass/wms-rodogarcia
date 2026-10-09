package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Doc29:53/88/93: insumos validos19,6 e resultado19,2, motor publico isolado. */
class D29FinanceiroLimitesTest {
    final D29FinanceiroOraculosTest f;

    D29FinanceiroLimitesTest() throws Exception {
        f = new D29FinanceiroOraculosTest();
    }

    @Test
    void meioCentavoArredondaParaUmCentavoSemDouble() {
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "1", false, "0.005", List.of(11L));
        var r = f.calculate(1);
        assertThat(r.pendencias()).isEmpty();
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.total()).isEqualByComparingTo("0.01");
        assertThat(r.memoria().servicos().getFirst().quantidade()).isEqualByComparingTo("0.005");
    }

    @Test
    void tarifa19Por6ValidaProduzMaximoMonetario19Por2Exato() {
        f.contract(null, "NAO_APLICAVEL", null);
        f.service(5, "9999999999999.999999", false, "10000", List.of(11L));
        var r = f.calculate(1);
        assertThat(r.pendencias()).isEmpty();
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.total()).isEqualByComparingTo("99999999999999999.99");
        assertThat(r.subtotalConhecido()).isEqualByComparingTo("99999999999999999.99");
        assertThat(r.memoria().servicos().getFirst().preco())
                .isEqualByComparingTo("9999999999999.999999");
    }

    @Test
    void subtotalElegivelExatamenteCinquentaNaoSomaMinimoOutraVez() {
        f.contract("50", "NAO_APLICAVEL", null);
        f.service(5, "25", true, "2", List.of(11L));
        var r = f.calculate(1);
        assertThat(r.pendencias()).isEmpty();
        assertThat(r.situacao()).isEqualTo("COMPLETO");
        assertThat(r.subtotalConhecido()).isEqualByComparingTo("50");
        assertThat(r.total()).isEqualByComparingTo("50");
    }
}
