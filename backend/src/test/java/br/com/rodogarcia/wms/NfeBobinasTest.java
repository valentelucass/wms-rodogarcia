package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.dto.RecebimentoDto;
import br.com.rodogarcia.wms.dto.UnidadeLogisticaDto;
import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import br.com.rodogarcia.wms.models.CondicaoMercadoria;
import br.com.rodogarcia.wms.models.TipoUnidadeLogistica;
import br.com.rodogarcia.wms.repositories.AuditoriaCadastroRepository;
import br.com.rodogarcia.wms.services.ConferenciaBobinasService;
import br.com.rodogarcia.wms.services.ConversaoUnidadeNfe;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

/** Regras puras e snapshots em memória; sem contexto, conexão ou processo de aplicação. */
class NfeBobinasTest {
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
