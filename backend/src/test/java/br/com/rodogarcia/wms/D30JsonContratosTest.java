package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.config.JsonInteirosModule;
import br.com.rodogarcia.wms.controllers.CalculoCobrancaController;
import br.com.rodogarcia.wms.controllers.ClienteController;
import br.com.rodogarcia.wms.controllers.PedidoSaidaController;
import br.com.rodogarcia.wms.dto.CalculoCobrancaDto;
import br.com.rodogarcia.wms.dto.ClienteDto;
import br.com.rodogarcia.wms.dto.PedidoSaidaDto;
import br.com.rodogarcia.wms.exceptions.ApiExceptionHandler;
import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import br.com.rodogarcia.wms.models.SituacaoCadastro;
import br.com.rodogarcia.wms.services.CalculoCobrancaService;
import br.com.rodogarcia.wms.services.ClienteService;
import br.com.rodogarcia.wms.services.PedidoSaidaService;
import br.com.rodogarcia.wms.services.PedidoSaidaXmlService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

/** Mapper/configuracao reais; controllers reais com servicos mockados, sem DataSource ou rede. */
class D30JsonContratosTest {
    private JsonMapper mapper;
    private MockMvc mvc;
    private ClienteService clientes;
    private PedidoSaidaService pedidos;
    private PedidoSaidaXmlService xml;
    private CalculoCobrancaService calculos;

    @BeforeEach
    void preparar() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withUserConfiguration(JsonInteirosModule.class)
                .withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                            mapper = context.getBean(JsonMapper.class);
                        });
        clientes = mock(ClienteService.class);
        pedidos = mock(PedidoSaidaService.class);
        xml = mock(PedidoSaidaXmlService.class);
        calculos = mock(CalculoCobrancaService.class);
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new ClienteController(clientes),
                                new PedidoSaidaController(pedidos, xml),
                                new CalculoCobrancaController(calculos))
                        .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper))
                        .setControllerAdvice(new ApiExceptionHandler(new ProblemasApi(mapper)))
                        .addFilters(new IdentificacaoOperacaoFilter())
                        .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"9007199254740993", "9223372036854775807"})
    void longPreservaLexemaRespostaEParametroSemDouble(String literal) throws Exception {
        long valor = Long.parseLong(literal);
        var resposta =
                new ClienteDto.Resposta(
                        valor,
                        valor,
                        SituacaoCadastro.ATIVO,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        null,
                        "D30",
                        "Ficticio",
                        "11111111000111");
        when(clientes.consultar(valor)).thenReturn(resposta);
        String body =
                mvc.perform(get("/api/v1/clientes/" + literal))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(body).contains("\"id\":" + literal, "\"versao\":" + literal);
        assertThat(mapper.readValue(body, ClienteDto.Resposta.class)).isEqualTo(resposta);
        verify(clientes).consultar(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9007199254740993", "9223372036854775807", "0"})
    void versaoEntradaPreservaInteiroExatoInclusiveZeroPermitido(String literal) throws Exception {
        mvc.perform(
                        put("/api/v1/clientes/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"versao\":"
                                                + literal
                                                + ",\"nome\":\"Ficticio\",\"motivo\":\"Caso local D30\"}"))
                .andExpect(status().isOk());
        var captor = ArgumentCaptor.forClass(ClienteDto.Alterar.class);
        verify(clientes).alterar(org.mockito.ArgumentMatchers.eq(1L), captor.capture());
        assertThat(captor.getValue().versao()).isEqualTo(Long.parseLong(literal));
    }

    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "null", "-1"})
    void versaoInvalidaRetorna400SeguroSemNegocio(String literal) throws Exception {
        String body =
                mvc.perform(
                                put("/api/v1/clientes/1")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"versao\":"
                                                        + literal
                                                        + ",\"nome\":\"Ficticio\",\"motivo\":\"Caso local D30\"}"))
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(body).doesNotContain("NumberFormatException", "trace", "Caso local D30");
        verifyNoInteractions(clientes, pedidos, xml, calculos);
    }

    @Test
    void idAcimaLongMaxRecusadoAntesDoServico() throws Exception {
        mvc.perform(get("/api/v1/clientes/9223372036854775808")).andExpect(status().isBadRequest());
        verifyNoInteractions(clientes, pedidos, xml, calculos);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9999999999999.999999", "0.000001", "9.999999999999999999E12"})
    void quantidadeEntradaPreservaCoeficienteEValorDoContrato(String literal) throws Exception {
        mvc.perform(
                        post("/api/v1/pedidos-saida")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(pedido(literal)))
                .andExpect(status().isCreated());
        var captor = ArgumentCaptor.forClass(PedidoSaidaDto.Criar.class);
        verify(pedidos).criar(captor.capture());
        assertThat(captor.getValue().itens()).hasSize(1);
        var quantidade = captor.getValue().itens().getFirst().quantidade();
        assertThat(quantidade).isEqualByComparingTo(new BigDecimal(literal));
        // BigDecimal das sequencias de referencia, sem double nem calculo produtivo.
        if (!literal.equals("0.000001")) {
            assertThat(quantidade.stripTrailingZeros().unscaledValue().toString())
                    .isEqualTo("9999999999999999999");
            assertThat(quantidade.stripTrailingZeros().scale()).isEqualTo(6);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"10000000000000.000000", "0.0000001", "null", "0", "-0.000001"})
    void quantidadeInvalidaRetorna400AntesDeEfeitos(String literal) throws Exception {
        String body =
                mvc.perform(
                                post("/api/v1/pedidos-saida")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(pedido(literal)))
                        .andExpect(status().isBadRequest())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(body)
                .contains("DADOS_INVALIDOS", "itens[0].quantidade")
                .doesNotContain("trace", "valor", "Caso local D30");
        verifyNoInteractions(clientes, pedidos, xml, calculos);
    }

    @ParameterizedTest
    @CsvSource({"99999999999999999.99,COMPLETO", "0.00,COMPLETO", "NULL,PENDENTE"})
    void moedaMaximaZeroENuloMantemDistincaoNaResposta(String literal, String situacao)
            throws Exception {
        BigDecimal valor = literal.equals("NULL") ? null : new BigDecimal(literal);
        var resposta =
                new CalculoCobrancaDto.Resultado(
                        1L,
                        1L,
                        2L,
                        3L,
                        LocalDate.of(2026, 9, 1),
                        LocalDate.of(2026, 9, 1),
                        "UTC",
                        "BRL",
                        situacao,
                        1,
                        "hash-ficticio",
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        valor,
                        Instant.parse("2026-09-02T00:00:00Z"),
                        List.of(),
                        new CalculoCobrancaDto.Memoria(List.of(), List.of(), null));
        when(calculos.consultar(1L)).thenReturn(resposta);
        String body =
                mvc.perform(get("/api/v1/calculos-cobranca/1"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var relido = mapper.readValue(body, CalculoCobrancaDto.Resultado.class);
        assertThat(relido.situacao()).isEqualTo(situacao);
        if (valor == null) {
            assertThat(body).contains("\"total\":null");
            assertThat(relido.total()).isNull();
        } else {
            assertThat(relido.total()).isEqualByComparingTo(valor);
            assertThat(relido.total().unscaledValue()).isEqualTo(valor.unscaledValue());
            assertThat(relido.total().scale()).isEqualTo(valor.scale());
        }
        verify(calculos).consultar(1L);
    }

    private static String pedido(String quantidade) {
        return "{\"operacaoId\":\"d3000000-0000-0000-0000-000000000001\",\"clienteId\":1,\"armazemId\":2,"
                + "\"referencia\":\"D30\",\"itens\":[{\"produtoId\":1,\"quantidade\":"
                + quantidade
                + "}],\"motivo\":\"Caso local D30\"}";
    }
}
