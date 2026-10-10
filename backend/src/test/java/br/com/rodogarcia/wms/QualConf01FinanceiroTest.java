package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Exemplos fictícios da proposta AC06, com calculador real e fontes isoladas; sem cobrança externa.
 */
class QualConf01FinanceiroTest {
    @ParameterizedTest
    @CsvSource({"49.99,56.99,0.01,57.00", "50.00,57.00,0.00,57.00", "50.01,57.01,0.00,57.01"})
    void minimoAc06CobreSoDiferencaEMantemServicoExcluido(
            String elegivel, String subtotal, String ajuste, String total) throws Exception {
        var f = new D29FinanceiroOraculosTest();
        f.contract("50", "APLICAVEL", "INTEGRAL");
        f.service(5, elegivel, true, "1", List.of(11L));
        f.service(6, "7", false, "1", List.of(12L));
        var resultado = f.calculate(1);
        assertThat(resultado.pendencias()).isEmpty();
        assertThat(resultado.situacao()).isEqualTo("COMPLETO");
        assertThat(resultado.subtotalConhecido()).isEqualByComparingTo(subtotal);
        assertThat(resultado.memoria().ajustes().subtotalElegivel()).isEqualByComparingTo(elegivel);
        assertThat(resultado.minimoCalculado()).isEqualByComparingTo(ajuste);
        assertThat(resultado.total()).isEqualByComparingTo(total);
    }
}
