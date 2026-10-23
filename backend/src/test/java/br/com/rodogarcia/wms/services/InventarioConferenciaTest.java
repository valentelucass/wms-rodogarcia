package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.models.InventarioItem;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Apenas records e BigDecimal: não abre contexto Spring, JDBC ou banco. */
class InventarioConferenciaTest {
    private static final Instant AGORA = Instant.parse("2026-10-23T12:00:00Z");

    private InventarioItem.Base base(
            String codigo, long versao, String quantidade, List<InventarioItem.Posicao> posicoes) {
        return new InventarioItem.Base(
                1L,
                codigo,
                versao,
                17L,
                "SKU-17",
                "Produto",
                "UN",
                6,
                new BigDecimal(quantidade),
                posicoes);
    }

    private InventarioItem.Posicao pos(long id) {
        return new InventarioItem.Posicao(id, "A-" + id);
    }

    private InventarioItem lida(InventarioItem.Base b, String contado, Long endereco) {
        return new InventarioItem(
                b,
                true,
                new BigDecimal(contado),
                endereco,
                endereco == null ? null : "A-" + endereco,
                AGORA,
                "operador");
    }

    @Test
    void quantidadeCorretaNaPosicaoCorretaNaoGeraDivergencia() {
        var b = base("um", 1, "10", List.of(pos(71)));
        var linhas = InventarioConferencia.conferir(List.of(lida(b, "10", 71L)), List.of(b));
        assertThat(linhas.getFirst().ocorrencias()).isEmpty();
        assertThat(linhas.getFirst().diferenca()).isEqualByComparingTo("0");
    }

    @Test
    void unidadeNaoBipadaEntraComoFaltanteMesmoSemLeitura() {
        var b = base("faltante", 1, "10.000001", List.of(pos(71)));
        var linhas =
                InventarioConferencia.conferir(
                        List.of(new InventarioItem(b, true, null, null, null, null, null)),
                        List.of(b));
        assertThat(linhas.getFirst().ocorrencias()).containsExactly("NAO_LIDA", "QUANTIDADE");
        assertThat(linhas.getFirst().diferenca()).isEqualByComparingTo("-10.000001");
    }

    @Test
    void zeroInformadoEhLeituraValidaNaoAusenciaDeLeitura() {
        var b = base("zero", 1, "10", List.of(pos(71)));
        var linhas = InventarioConferencia.conferir(List.of(lida(b, "0", null)), List.of(b));
        assertThat(linhas.getFirst().ocorrencias()).containsExactly("QUANTIDADE");
    }

    @Test
    void mesmaQuantidadeNaPosicaoErradaContinuaDivergente() {
        var b = base("errada", 1, "10", List.of(pos(71)));
        var linhas = InventarioConferencia.conferir(List.of(lida(b, "10", 72L)), List.of(b));
        assertThat(linhas.getFirst().ocorrencias()).containsExactly("ENDERECO");
    }

    @Test
    void unidadeQueOcupaDuasPosicoesEhContadaUmaVez() {
        var b = base("dupla", 1, "10", List.of(pos(71), pos(72)));
        var linhas = InventarioConferencia.conferir(List.of(lida(b, "10", 72L)), List.of(b));
        assertThat(linhas.getFirst().ocorrencias()).isEmpty();
        assertThat(InventarioConferencia.porSku(linhas).getFirst().esperado())
                .isEqualByComparingTo("10");
    }

    @Test
    void trocaDePosicaoDuranteAContagemExigeRecontagem() {
        var b = base("movida", 1, "10", List.of(pos(71)));
        var atual = base("movida", 2, "10", List.of(pos(72)));
        assertThat(
                        InventarioConferencia.conferir(List.of(lida(b, "10", 71L)), List.of(atual))
                                .getFirst()
                                .ocorrencias())
                .containsExactly("MOVIMENTACAO");
    }

    @Test
    void mercadoriaRetiradaDuranteAContagemEhSinalizada() {
        var b = base("retirada", 1, "10", List.of(pos(71)));
        assertThat(
                        InventarioConferencia.conferir(List.of(lida(b, "10", 71L)), List.of())
                                .getFirst()
                                .ocorrencias())
                .containsExactly("MOVIMENTACAO");
    }

    @Test
    void novaEntradaNaoLidaNaoSomeDoRelatorio() {
        var nova = base("nova", 1, "10", List.of(pos(71)));
        var linha = InventarioConferencia.conferir(List.of(), List.of(nova)).getFirst();
        assertThat(linha.ocorrencias()).containsExactly("MOVIMENTACAO");
        assertThat(linha.esperado()).isEqualByComparingTo("0");
    }

    @Test
    void mercadoriaEncontradaForaDaBaseEhExcessoSemAlterarBase() {
        var b = base("extra", 1, "10", List.of(pos(71)));
        var item =
                new InventarioItem(b, false, new BigDecimal("10"), 72L, "A-72", AGORA, "operador");
        var linha = InventarioConferencia.conferir(List.of(item), List.of(b)).getFirst();
        assertThat(linha.ocorrencias())
                .containsExactly("QUANTIDADE", "ENDERECO", "FORA_BASE", "MOVIMENTACAO");
        assertThat(linha.esperado()).isEqualByComparingTo("0");
        assertThat(linha.diferenca()).isEqualByComparingTo("10");
    }

    @Test
    void mercadoriaNaPosicaoErradaNaoInventaMovimentacaoDuranteAContagem() {
        var b = base("fora", 1, "10", List.of(pos(71)));
        var item =
                new InventarioItem(b, false, new BigDecimal("10"), 72L, "A-72", AGORA, "operador");
        var linha = InventarioConferencia.conferir(List.of(item), List.of()).getFirst();
        assertThat(linha.ocorrencias()).containsExactly("QUANTIDADE", "ENDERECO", "FORA_BASE");
    }

    @Test
    void divergenciasOpostasDoMesmoSkuNaoEscondemMercadoriasComErro() {
        var a = base("a", 1, "10", List.of(pos(71)));
        var b = base("b", 1, "10", List.of(pos(72)));
        var linhas =
                InventarioConferencia.conferir(
                        List.of(lida(a, "8", 71L), lida(b, "12", 72L)), List.of(a, b));
        var grupo = InventarioConferencia.porSku(linhas).getFirst();
        assertThat(grupo.diferenca()).isEqualByComparingTo("0");
        assertThat(grupo.mercadoriasDivergentes()).isEqualTo(2);
    }

    @Test
    void quantidadeAlteradaMesmoSemTrocaDePosicaoNaoPassaDespercebida() {
        var b = base("quantidade", 1, "10", List.of(pos(71)));
        var atual = base("quantidade", 1, "9", List.of(pos(71)));
        assertThat(
                        InventarioConferencia.conferir(List.of(lida(b, "10", 71L)), List.of(atual))
                                .getFirst()
                                .ocorrencias())
                .containsExactly("MOVIMENTACAO");
    }

    @Test
    void decimaisSaoConferidosSemArredondamentoBinario() {
        var b = base("precisa", 1, "9999999999999.999999", List.of());
        var linha =
                InventarioConferencia.conferir(
                                List.of(lida(b, "9999999999999.999998", null)), List.of(b))
                        .getFirst();
        assertThat(linha.diferenca()).isEqualByComparingTo("-0.000001");
    }
}
