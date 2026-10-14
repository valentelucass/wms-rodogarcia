package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.PedidoEntradaDto;
import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.models.Armazem;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.models.Cliente;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.ItemNotaEntrada;
import br.com.rodogarcia.wms.models.NotaEntrada;
import br.com.rodogarcia.wms.models.PedidoEntrada;
import br.com.rodogarcia.wms.models.Produto;
import br.com.rodogarcia.wms.models.TipoQuantidade;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.repositories.ItemChegadaRepository;
import br.com.rodogarcia.wms.repositories.ItemNotaEntradaRepository;
import br.com.rodogarcia.wms.repositories.NotaEntradaRepository;
import br.com.rodogarcia.wms.services.ConferenciaBobinasService;
import br.com.rodogarcia.wms.services.ConversaoUnidadeNfe;
import br.com.rodogarcia.wms.services.OrigemEntradaService;
import br.com.rodogarcia.wms.services.PedidoEntradaService;
import br.com.rodogarcia.wms.services.RegraBobinaPorItem;
import br.com.rodogarcia.wms.services.RegraSkuXml;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

/** Regras puras e snapshots em memória; sem contexto, conexão ou processo de aplicação. */
class NfeBobinasTest {
    @ParameterizedTest
    @ValueSource(
            strings = {"kg", "Kg", "KG", "to", "To", "TO", "t", "T", "ton", "Ton", "TON", " KG "})
    void regraBobinaReconheceGrafiasDeMassaSemAlterarPar(String unidade) {
        var regra = new RegraBobinaPorItem("33064262000250:BOBINA-TESTE");
        assertThat(regra.aplica("33064262000250", "BOBINA-TESTE", unidade)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
        "33064262000250,BOBINA-TESTE,UN",
        "33064262000250,BOBINA-TESTE,un",
        "33064262000250,OUTRO,kg",
        "00000000000000,BOBINA-TESTE,Kg"
    })
    void normalizarUnidadeNaoAmpliaProdutosClientesOuContagem(
            String emitente, String sku, String unidade) {
        var regra = new RegraBobinaPorItem("33064262000250:BOBINA-TESTE");
        assertThat(regra.aplica(emitente, sku, unidade)).isFalse();
    }

    @Test
    void grafiaMassaPreservaRegraSkuZerosConfiguradaELiteralDesativada() {
        String emitente = "33064262000250";
        var regra = new RegraBobinaPorItem(emitente + ":000600366", new RegraSkuXml(emitente));
        for (String sku : List.of("600366", "000600366", "000000000000600366")) {
            assertThat(regra.aplica(emitente, sku, "Kg")).isTrue();
            assertThat(regra.aplica(emitente, sku, "UN")).isFalse();
        }
        assertThat(regra.aplica(emitente, "600367", "kg")).isFalse();
        assertThat(new RegraBobinaPorItem(emitente + ":000600366").aplica(emitente, "600366", "KG"))
                .isFalse();
    }

    @Test
    void detalheSerializadoUsaUnidadeDeCadaProdutoAposConversaoSemInferirPeloPeso() {
        var cliente = mock(Cliente.class);
        var armazem = mock(Armazem.class);
        var pedido = mock(PedidoEntrada.class);
        var nota = mock(NotaEntrada.class);
        when(pedido.getId()).thenReturn(42L);
        when(pedido.getCliente()).thenReturn(cliente);
        when(pedido.getArmazem()).thenReturn(armazem);
        when(nota.getId()).thenReturn(7L);
        var massa =
                spy(
                        new Produto(
                                cliente,
                                "PESO",
                                "Produto medido",
                                "KG",
                                TipoQuantidade.MEDIDA,
                                3,
                                false,
                                false,
                                null,
                                Instant.EPOCH));
        var contagem =
                spy(
                        new Produto(
                                cliente,
                                "PECAS",
                                "Produto contado",
                                "UN",
                                TipoQuantidade.CONTAGEM,
                                0,
                                false,
                                false,
                                null,
                                Instant.EPOCH));
        when(massa.getId()).thenReturn(21L);
        when(contagem.getId()).thenReturn(22L);
        var peso = mock(ItemNotaEntrada.class);
        var pecas = mock(ItemNotaEntrada.class);
        when(peso.getId()).thenReturn(31L);
        when(peso.getNota()).thenReturn(nota);
        when(peso.getProduto()).thenReturn(massa);
        when(peso.getQuantidadePrevista())
                .thenReturn(ConversaoUnidadeNfe.converter(new BigDecimal("1.488"), "TO", "KG"));
        when(pecas.getId()).thenReturn(32L);
        when(pecas.getNota()).thenReturn(nota);
        when(pecas.getProduto()).thenReturn(contagem);
        when(pecas.getQuantidadePrevista()).thenReturn(new BigDecimal("4"));
        var recebidoPeso = mock(ItemChegadaRepository.QuantidadeRecebida.class);
        when(recebidoPeso.getItemId()).thenReturn(31L);
        when(recebidoPeso.getBoa()).thenReturn(new BigDecimal("1000"));
        when(recebidoPeso.getAvariada()).thenReturn(new BigDecimal("12"));
        when(recebidoPeso.getPrimeiraChegada()).thenReturn(Instant.EPOCH);
        var recebidoPecas = mock(ItemChegadaRepository.QuantidadeRecebida.class);
        when(recebidoPecas.getItemId()).thenReturn(32L);
        when(recebidoPecas.getBoa()).thenReturn(new BigDecimal("2"));
        when(recebidoPecas.getAvariada()).thenReturn(BigDecimal.ZERO);
        when(recebidoPecas.getPrimeiraChegada()).thenReturn(Instant.EPOCH);
        var notas = mock(NotaEntradaRepository.class);
        var itens = mock(ItemNotaEntradaRepository.class);
        var fisicos = mock(ItemChegadaRepository.class);
        var origens = mock(OrigemEntradaService.class);
        when(notas.findByPedidoIdOrderById(42L)).thenReturn(List.of(nota));
        when(itens.buscarDoPedido(42L)).thenReturn(List.of(peso, pecas));
        when(fisicos.resumir(42L)).thenReturn(List.of(recebidoPeso, recebidoPecas));
        when(origens.consultar(List.of(pedido))).thenReturn(Map.of());
        var service =
                new PedidoEntradaService(
                        null,
                        notas,
                        itens,
                        fisicos,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Clock.systemUTC(),
                        origens,
                        new RegraBobinaPorItem(""));
        var mapper = JsonMapper.builder().build();
        var json =
                mapper.readTree(
                        mapper.writeValueAsString(
                                service.detalhe(pedido).notas().getFirst().itens()));
        assertThat(json.get(0).path("unidadeMedida").asString()).isEqualTo("KG");
        assertThat(json.get(0).get("prevista").decimalValue()).isEqualByComparingTo("1488");
        assertThat(json.get(0).get("recebidaBoa").decimalValue()).isEqualByComparingTo("1000");
        assertThat(json.get(0).get("recebidaAvariada").decimalValue()).isEqualByComparingTo("12");
        assertThat(json.get(0).get("diferenca").decimalValue()).isEqualByComparingTo("-476");
        assertThat(json.get(1).path("unidadeMedida").asString()).isEqualTo("UN");
        assertThat(json.get(1).get("prevista").decimalValue()).isEqualByComparingTo("4");
        assertThat(json.get(1).get("recebidaBoa").decimalValue()).isEqualByComparingTo("2");
        assertThat(json.get(1).get("recebidaAvariada").decimalValue()).isZero();
        assertThat(json.get(1).get("diferenca").decimalValue()).isEqualByComparingTo("-2");
    }

    @Test
    void contratoDtoContemUnidadeMedidaStringAposSku() {
        var componentes = PedidoEntradaDto.ItemConferencia.class.getRecordComponents();
        assertThat(
                        java.util.Arrays.stream(componentes)
                                .map(java.lang.reflect.RecordComponent::getName))
                .containsSubsequence("sku", "unidadeMedida", "prevista");
        assertThat(
                        java.util.Arrays.stream(componentes)
                                .filter(c -> c.getName().equals("unidadeMedida")))
                .singleElement()
                .satisfies(c -> assertThat(c.getType()).isEqualTo(String.class));
    }

    @ParameterizedTest
    @CsvSource({
        "1.488,TO,KG,1488",
        "1.548,T,KG,1548",
        "1488,KG,TON,1.488",
        "12,KG,KG,12",
        "1,TON,TO,1"
    })
    void converteSomenteMassaSemArredondar(String q, String de, String para, String esperado) {
        assertThat(ConversaoUnidadeNfe.converter(new BigDecimal(q), de, para))
                .isEqualByComparingTo(esperado);
    }

    @Test
    void naoConvertePesoEmContagem() {
        assertThatThrownBy(() -> ConversaoUnidadeNfe.converter(BigDecimal.ONE, "TO", "UN"))
                .hasMessageContaining("incompatíveis");
    }

    private RecebimentoDto.Item item(int boas, int avariadas, String pesoBom, String pesoAvariado) {
        return new RecebimentoDto.Item(
                1L,
                null,
                null,
                new BigDecimal(pesoBom),
                new BigDecimal(pesoAvariado),
                new RecebimentoDto.Bobinas(boas, avariadas));
    }

    @Test
    void pesoEContagemSaoIndependentes() {
        assertThatCode(() -> ConferenciaBobinasService.validar(item(2, 1, "1488", "60")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ConferenciaBobinasService.validar(item(0, 1, "1488", "60")))
                .hasMessageContaining("boas e avariadas");
        assertThatThrownBy(() -> ConferenciaBobinasService.validar(item(101, 0, "12", "0")))
                .hasMessageContaining("100 bobinas");
    }

    @Test
    void distribuicaoConfereContagemECondicao() {
        var boa =
                new UnidadeLogisticaDto.NovaUnidade(
                        1L,
                        TipoUnidadeLogistica.BOBINA,
                        CondicaoMercadoria.BOA,
                        new BigDecimal("5"));
        var avaria =
                new UnidadeLogisticaDto.NovaUnidade(
                        1L,
                        TipoUnidadeLogistica.BOBINA,
                        CondicaoMercadoria.AVARIADA,
                        new BigDecimal("7"));
        assertThatCode(
                        () ->
                                ConferenciaBobinasService.conferir(
                                        new RecebimentoDto.Bobinas(1, 1), List.of(boa, avaria)))
                .doesNotThrowAnyException();
        assertThatThrownBy(
                        () ->
                                ConferenciaBobinasService.conferir(
                                        new RecebimentoDto.Bobinas(2, 0), List.of(boa, avaria)))
                .hasMessageContaining("contagem física");
        var pallet =
                new UnidadeLogisticaDto.NovaUnidade(
                        1L,
                        TipoUnidadeLogistica.PALLET,
                        CondicaoMercadoria.BOA,
                        new BigDecimal("12"));
        assertThatThrownBy(
                        () ->
                                ConferenciaBobinasService.conferir(
                                        new RecebimentoDto.Bobinas(1, 0), List.of(pallet)))
                .hasMessageContaining("contagem física");
    }

    @Test
    void comandoAntigoMantemSerializacaoSemCampoNovo() {
        var mapper = JsonMapper.builder().build();
        var antigo = new RecebimentoDto.Item(1L, null, null, BigDecimal.ONE, BigDecimal.ZERO);
        assertThat(mapper.writeValueAsString(antigo)).doesNotContain("bobinas");
        assertThatCode(() -> ConferenciaBobinasService.validar(antigo)).doesNotThrowAnyException();
        assertThatCode(() -> ConferenciaBobinasService.conferir(null, List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void leContagemPorChegadaESequenciaSemInventarHistorico() {
        var repository = mock(AuditoriaCadastroRepository.class);
        var snapshot =
                """
                {"evento":{"chegadaId":7,"dados":{"itens":[{"bobinas":{"boas":2,"avariadas":1}},{"quantidadeBoa":10}]}}}
                """;
        var historico =
                """
                {"evento":{"chegadaId":8,"dados":{"itens":[{"quantidadeBoa":10}]}}}
                """;
        when(repository.findByTipoAndRegistroIdAndAcaoOrderByIdAsc(
                        "PEDIDO_ENTRADA", 42L, "CHEGADA_REGISTRADA"))
                .thenReturn(List.of(auditoria(snapshot), auditoria(historico)));
        var service = new ConferenciaBobinasService(repository, JsonMapper.builder().build());
        var resultado = service.consultar(42L);
        assertThat(resultado).containsOnlyKeys(7L);
        assertThat(resultado.get(7L)).containsOnlyKeys(1);
        assertThat(resultado.get(7L).get(1)).isEqualTo(new RecebimentoDto.Bobinas(2, 1));
    }

    private AuditoriaCadastro auditoria(String json) {
        return new AuditoriaCadastro(
                "PEDIDO_ENTRADA",
                42L,
                "CHEGADA_REGISTRADA",
                "ficticio",
                Instant.EPOCH,
                "00000000-0000-0000-0000-000000000001",
                "teste ficticio",
                null,
                json);
    }
}
