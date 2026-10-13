package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.NfeImportacaoDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.repositories.ArmazemRepository;
import br.com.rodogarcia.wms.repositories.ClienteRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.OperacaoAdministrativaRepository;
import br.com.rodogarcia.wms.repositories.ProdutoRepository;
import br.com.rodogarcia.wms.services.AcessoService;
import br.com.rodogarcia.wms.services.NfeDocumentoService;
import br.com.rodogarcia.wms.services.NfeXmlService;
import br.com.rodogarcia.wms.services.NotaEntradaService;
import br.com.rodogarcia.wms.services.OperacaoAdministrativaService;
import br.com.rodogarcia.wms.services.PedidoEntradaService;
import br.com.rodogarcia.wms.services.PedidoEntradaXmlService;
import br.com.rodogarcia.wms.services.RegraBobinaPorItem;
import br.com.rodogarcia.wms.services.RegraSkuXml;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

/** Sem contexto Spring, SQL, H2 ou aplicação; repositórios apenas dublês em memória. */
class NfeSkuTest {
    private static final String CNPJ = "33064262000250";

    @ParameterizedTest
    @ValueSource(strings = {"600366", "000600366", "000000000000600366"})
    void variantesNosDoisSentidosPreservamZerosInternos(String sku) {
        var valores = new RegraSkuXml(CNPJ).variantes(CNPJ, sku);
        assertThat(valores).contains("600366", "000600366", "000000000000600366");
        assertThat(valores).hasSize(35).doesNotContain("6366");
        assertThat(valores).allMatch(s -> s.length() <= 40);
    }

    @ParameterizedTest
    @ValueSource(strings = {"000ABC", "00012A", "000", "0", "0001-2", "0001.2"})
    void outrosCodigosPermanecemLiterais(String sku) {
        assertThat(new RegraSkuXml(CNPJ).variantes(CNPJ, sku)).containsExactly(sku);
    }

    @Test
    void configuracaoVaziaEOutroClienteNaoAtivamRegra() {
        assertThat(new RegraSkuXml("").variantes(CNPJ, "000123")).containsExactly("000123");
        assertThat(new RegraSkuXml("12345678901234").variantes(CNPJ, "000123"))
                .containsExactly("000123");
        assertThatThrownBy(() -> new RegraSkuXml("*")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void codigoLongoNaoSofreOverflowNemTruncamento() {
        String longo = "9".repeat(40);
        assertThat(new RegraSkuXml(CNPJ).variantes(CNPJ, "000" + longo)).containsExactly(longo);
        assertThat(new RegraSkuXml(CNPJ).variantes(CNPJ, longo + "9")).containsExactly(longo + "9");
    }

    @Test
    void sugestaoBobinaAcompanhaRegraSemAmpliarClienteOuUnidade() {
        var bobinas = new RegraBobinaPorItem(CNPJ + ":000600366", new RegraSkuXml(CNPJ));
        assertThat(bobinas.aplica(CNPJ, "600366", "TO")).isTrue();
        assertThat(bobinas.aplica(CNPJ, "600366", "UN")).isFalse();
        assertThat(bobinas.aplica("12345678901234", "600366", "TO")).isFalse();
        assertThat(bobinas.aplica(CNPJ, "600367", "TO")).isFalse();
    }

    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final Cliente cliente = mock(Cliente.class);
    private final Produto produto = mock(Produto.class);
    private String xml;

    private PedidoEntradaXmlService servico(String configuracao) throws Exception {
        xml = NfeDocumentoServiceTest.exportacao("uma").replace("BOBINA-TESTE", "000600366");
        var clientes = mock(ClienteRepository.class);
        var armazens = mock(ArmazemRepository.class);
        var armazem = mock(Armazem.class);
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente));
        when(cliente.getId()).thenReturn(1L);
        when(cliente.getDocumentoFiscal()).thenReturn(CNPJ);
        when(cliente.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);
        when(armazens.findById(2L)).thenReturn(Optional.of(armazem));
        when(armazem.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);
        when(produtos.findById(7L)).thenReturn(Optional.of(produto));
        when(produto.getId()).thenReturn(7L);
        when(produto.getCliente()).thenReturn(cliente);
        when(produto.getSituacao()).thenReturn(SituacaoCadastro.ATIVO);
        when(produto.getSku()).thenReturn("600366");
        when(produto.getUnidadeMedida()).thenReturn("KG");
        when(produto.getTipoQuantidade()).thenReturn(TipoQuantidade.MEDIDA);
        when(produto.getPrecisaoQuantidade()).thenReturn(3);
        return new PedidoEntradaXmlService(
                new NfeDocumentoService(new NfeXmlService()),
                clientes,
                armazens,
                produtos,
                mock(NotaEntradaRepository.class),
                mock(OperacaoAdministrativaRepository.class),
                mock(OperacaoAdministrativaService.class),
                mock(PedidoEntradaService.class),
                mock(NotaEntradaService.class),
                mock(AcessoService.class),
                JsonMapper.builder().build(),
                "1",
                new RegraSkuXml(configuracao));
    }

    @Test
    void associaCorrespondenciaUnicaSemAlterarCodigoDaNota() throws Exception {
        var service = servico(CNPJ);
        when(produtos.buscarIdsPorSkus(eq(1L), anyList(), any())).thenReturn(List.of(7L));
        var previa = service.previa(new NfeImportacaoDto.Ler(xml, 1L, 2L, List.of()));
        assertThat(previa.itens().getFirst().produtoId()).isEqualTo(7L);
        assertThat(previa.documento().itens().getFirst().codigo()).isEqualTo("000600366");
        assertThat(previa.itens().getFirst().sku()).isEqualTo("600366");
        verify(produtos)
                .buscarIdsPorSkus(
                        eq(1L),
                        argThat(s -> s.contains("600366") && s.contains("000600366")),
                        argThat(p -> p.getPageSize() == 2));
    }

    @Test
    void ambiguoNaoEscolheNemOMatchExato() throws Exception {
        var service = servico(CNPJ);
        when(produtos.buscarIdsPorSkus(eq(1L), anyList(), any())).thenReturn(List.of(7L, 8L));
        var previa = service.previa(new NfeImportacaoDto.Ler(xml, 1L, 2L, List.of()));
        assertThat(previa.podeConfirmar()).isFalse();
        assertThat(previa.itens().getFirst().produtoId()).isNull();
        assertThat(previa.itens().getFirst().pendencia()).contains("Mais de um produto");
        verify(produtos, never()).findById(anyLong());
    }

    @Test
    void associacaoManualPrevaleceSobreBusca() throws Exception {
        var service = servico(CNPJ);
        var previa =
                service.previa(
                        new NfeImportacaoDto.Ler(
                                xml, 1L, 2L, List.of(new NfeImportacaoDto.Associacao(1, 7L))));
        assertThat(previa.itens().getFirst().produtoId()).isEqualTo(7L);
        verify(produtos, never()).buscarIdsPorSkus(anyLong(), anyList(), any());
    }

    @Test
    void desativadaMantemBuscaExata() throws Exception {
        var service = servico("");
        service.previa(new NfeImportacaoDto.Ler(xml, 1L, 2L, List.of()));
        verify(produtos).buscarIdPorSku(1L, "000600366");
        verify(produtos, never()).buscarIdsPorSkus(anyLong(), anyList(), any());
    }

    @Test
    void clienteDivergenteNaoConsultaProdutos() throws Exception {
        var service = servico(CNPJ);
        when(cliente.getDocumentoFiscal()).thenReturn("12345678901234");
        service.previa(new NfeImportacaoDto.Ler(xml, 1L, 2L, List.of()));
        verifyNoInteractions(produtos);
    }
}
