package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.controllers.ClienteController;
import br.com.rodogarcia.wms.dto.RevisaoCadastroRequest;
import br.com.rodogarcia.wms.exceptions.ApiExceptionHandler;
import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import br.com.rodogarcia.wms.services.ClienteService;
import jakarta.persistence.EntityManagerFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;

/** Valores nao inteiros sao distintos de zero/versao1; mapper Boot/rota reais sem banco. */
class D30LongFracaoTest {
    static JsonMapper mapperLocal() {
        JsonMapper[] mapper = new JsonMapper[1];
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withInitializer(
                        context -> {
                            var scanner =
                                    new ClassPathBeanDefinitionScanner(
                                            (BeanDefinitionRegistry) context, false);
                            scanner.addIncludeFilter(new AssignableTypeFilter(JacksonModule.class));
                            scanner.scan("br.com.rodogarcia.wms.config");
                        })
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class))
                .withPropertyValues("spring.profiles.active=local")
                .run(
                        context -> {
                            assertThat(context).hasNotFailed();
                            assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                            assertThat(context.getBeansOfType(EntityManagerFactory.class))
                                    .isEmpty();
                            mapper[0] = context.getBean(JsonMapper.class);
                        });
        return mapper[0];
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.9", "0.9"})
    void versaoFracionariaNaoTruncaERecusaAntesDoNegocio(String literal) throws Exception {
        JsonMapper[] mapper = {mapperLocal()};
        var clientes = mock(ClienteService.class);
        var mvc =
                MockMvcBuilders.standaloneSetup(new ClienteController(clientes))
                        .setMessageConverters(new JacksonJsonHttpMessageConverter(mapper[0]))
                        .setControllerAdvice(new ApiExceptionHandler(new ProblemasApi(mapper[0])))
                        .addFilters(new IdentificacaoOperacaoFilter())
                        .build();
        String body = "{\"versao\":" + literal + ",\"motivo\":\"Caso fracao D30 sem truncamento\"}";
        RevisaoCadastroRequest[] lido = new RevisaoCadastroRequest[1];
        var erro =
                catchThrowable(
                        () -> lido[0] = mapper[0].readValue(body, RevisaoCadastroRequest.class));
        var response =
                mvc.perform(
                                post("/api/v1/clientes/1/encerramento")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andReturn()
                        .getResponse();
        var proof = new LinkedHashMap<String, Object>();
        proof.put("id", "D30-C-CASO-LONG-FRACAO-001");
        proof.put("entradaLiteral", literal);
        proof.put("mapperBoot", mapper[0].getClass().getName());
        proof.put(
                "featureDefaultAceitaFloat",
                mapper[0].isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT));
        proof.put("mapperVersaoLida", lido[0] == null ? null : lido[0].versao());
        proof.put("mapperErroTipo", erro == null ? null : erro.getClass().getName());
        proof.put("statusHTTP", response.getStatus());
        proof.put("invocacoesNegocio", mockingDetails(clientes).getInvocations().size());
        proof.put("DataSource", 0);
        proof.put("EntityManagerFactory", 0);
        proof.put("SQLServer", false);
        var dir = Path.of(System.getProperty("wms.test.evidencias.dir"));
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("d30-cedro-long-fracao-" + UUID.randomUUID() + ".json"),
                mapper[0].writerWithDefaultPrettyPrinter().writeValueAsString(proof),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(erro).isNotNull();
        assertThat(lido[0]).isNull();
        assertThat(response.getContentAsString())
                .doesNotContain(
                        "Caso fracao",
                        "NumberFormatException",
                        "ArithmeticException",
                        "trace",
                        "br.com");
        verifyNoInteractions(clientes);
    }
}
