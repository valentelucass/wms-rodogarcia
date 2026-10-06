package br.com.rodogarcia.wms.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc =
                MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                        .setControllerAdvice(
                                new ApiExceptionHandler(
                                        new ProblemasApi(JsonMapper.builder().build())))
                        .addFilters(new IdentificacaoOperacaoFilter())
                        .build();
    }

    @Test
    void rejeitaJsonMalformadoSemRepetirConteudoRecebido() throws Exception {
        var response =
                mvc.perform(
                                post("/teste")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"nome\": segredo-ficticio}"))
                        .andExpect(status().isBadRequest())
                        .andExpect(
                                content()
                                        .contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                        .andExpect(jsonPath("codigo").value("HTTP_400"))
                        .andReturn()
                        .getResponse();

        assertThat(response.getContentAsString())
                .doesNotContain("segredo-ficticio", "JsonParseException");
        assertThat(response.getContentAsString()).contains(response.getHeader("X-Request-Id"));
    }

    @Test
    void informaCampoERestricaoSemExporValorOuMensagemDeValidacao() throws Exception {
        mvc.perform(
                        post("/teste")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nome\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("codigo").value("DADOS_INVALIDOS"))
                .andExpect(jsonPath("campos[0].campo").value("nome"))
                .andExpect(jsonPath("campos[0].codigo").value("NotBlank"))
                .andExpect(jsonPath("campos[0].valor").doesNotExist());
    }

    @Test
    void sanitizaFalhaInesperadaEConservaCorrelacaoNosLogs(CapturedOutput output) throws Exception {
        var response =
                mvc.perform(get("/teste/falha?senha=segredo-ficticio"))
                        .andExpect(status().isInternalServerError())
                        .andExpect(jsonPath("codigo").value("ERRO_INTERNO"))
                        .andReturn()
                        .getResponse();

        assertThat(response.getContentAsString())
                .doesNotContain("segredo-ficticio", "IllegalStateException", "trace");
        assertThat(output.getAll())
                .contains(response.getHeader("X-Request-Id"), "IllegalStateException")
                .doesNotContain("segredo-ficticio");
        assertThat(MDC.get(IdentificacaoOperacaoFilter.ATRIBUTO)).isNull();
    }

    @Test
    void preservaStatusECabecalhoAllowDoProtocoloHttp() throws Exception {
        var response =
                mvc.perform(get("/teste"))
                        .andExpect(status().isMethodNotAllowed())
                        .andExpect(jsonPath("codigo").value("HTTP_405"))
                        .andReturn()
                        .getResponse();
        assertThat(response.getHeader("Allow")).contains("POST");
    }

    @Test
    void trataFormatoDeConteudoIncompativel() throws Exception {
        mvc.perform(post("/teste").contentType(MediaType.TEXT_PLAIN).content("segredo-ficticio"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("codigo").value("HTTP_415"));
    }

    @Test
    void sanitizaViolacaoDeContratoDoServico(CapturedOutput output) throws Exception {
        var response =
                mvc.perform(get("/teste/validacao-servico"))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("codigo").value("DADOS_INVALIDOS"))
                        .andReturn()
                        .getResponse();
        assertThat(response.getContentAsString())
                .doesNotContain("segredo-ficticio", "ConstraintViolationException");
        assertThat(output.getAll()).doesNotContain("segredo-ficticio");
    }

    @RestController
    static class ControllerDeTeste {
        @GetMapping("/teste/validacao-servico")
        void violarContratoDoServico() {
            throw new ConstraintViolationException("segredo-ficticio", Set.of());
        }

        @PostMapping("/teste")
        DadosTeste validar(@Valid @RequestBody DadosTeste dados) {
            return dados;
        }

        @GetMapping("/teste/falha")
        void falhar() {
            throw new IllegalStateException("senha=segredo-ficticio");
        }
    }

    record DadosTeste(@NotBlank String nome) {}
}
