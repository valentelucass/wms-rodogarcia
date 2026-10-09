package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rodogarcia.wms.config.CadastrosSegurancaConfig;
import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.controllers.ClienteController;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import br.com.rodogarcia.wms.services.ClienteService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.json.JsonMapper;

/** Cadeia real, JWT RSA efêmero e MockServletContext; zero banco/servidor/JWKS externo. */
@SpringJUnitConfig(D30BearerCookieTest.Contexto.class)
@WebAppConfiguration
@ActiveProfiles("test")
@TestPropertySource("classpath:d30-cedro-security-test.properties")
class D30BearerCookieTest {
    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @EnableWebSecurity
    @Import({
        CadastrosSegurancaConfig.class,
        IdentidadeTesteConfig.class,
        JacksonAutoConfiguration.class
    })
    static class Contexto implements WebMvcConfigurer {
        private final JsonMapper mapper;

        Contexto(JsonMapper mapper) {
            this.mapper = mapper;
        }

        @Bean
        static BeanFactoryPostProcessor guardaSemBanco() {
            return bf -> {
                Environment env = bf.getBean(Environment.class);
                if (!Arrays.equals(env.getActiveProfiles(), new String[] {"test"})
                        || bf.getBeanNamesForType(DataSource.class, false, false).length != 0
                        || bf.getBeanNamesForType(EntityManagerFactory.class, false, false).length
                                != 0
                        || Arrays.stream(bf.getBeanDefinitionNames())
                                .anyMatch(n -> n.contains("SqlServerConfig"))
                        || !"false".equals(env.getProperty("spring.flyway.enabled"))
                        || !"never".equals(env.getProperty("spring.sql.init.mode"))) {
                    throw new IllegalStateException("Contexto D30 Cookie fora do canal sem banco.");
                }
                String dono = bf.getBeanDefinition("jwtDecoder").getFactoryBeanName();
                if (dono == null || !dono.endsWith("IdentidadeTesteConfig")) {
                    throw new IllegalStateException("Decoder D30 precisa da chave efêmera local.");
                }
            };
        }

        @Bean
        ClienteService clientes() {
            return mock(ClienteService.class);
        }

        @Bean
        ClienteController controller(ClienteService clientes) {
            return new ClienteController(clientes);
        }

        @Bean
        ProblemasApi problemas(JsonMapper mapper) {
            return new ProblemasApi(mapper);
        }

        @Override
        public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
            converters.add(new JacksonJsonHttpMessageConverter(mapper));
        }
    }

    @Autowired WebApplicationContext contexto;
    @Autowired JwtEncoder encoder;
    @Autowired ClienteService clientes;
    @Autowired JsonMapper mapper;

    @Test
    void mesmoTokenSoAutenticaNoBearerECookieNaoCriaSessao() throws Exception {
        assertThat(contexto.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(contexto.getBeansOfType(EntityManagerFactory.class)).isEmpty();
        Instant inicio = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .audience(List.of("wms-testes"))
                        .subject("Cedro-D30-ficticio")
                        .issuedAt(inicio)
                        .expiresAt(inicio.plusSeconds(300))
                        .claim("wms_perfil", "GESTOR")
                        .claim("wms_clientes", List.of())
                        .claim("wms_armazens", List.of())
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                        .getTokenValue();
        var mvc =
                MockMvcBuilders.webAppContextSetup(contexto)
                        .apply(springSecurity())
                        .addFilters(new IdentificacaoOperacaoFilter())
                        .build();
        var cookie = new Cookie("access_token", token);
        var semHeader =
                mvc.perform(get("/api/v1/clientes").cookie(cookie))
                        .andExpect(status().isUnauthorized())
                        .andReturn()
                        .getResponse();
        assertThat(mapper.readTree(semHeader.getContentAsString()).get("codigo").asString())
                .isEqualTo("NAO_AUTENTICADO");
        assertThat(semHeader.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        verifyNoInteractions(clientes);

        when(clientes.listar(0, 20)).thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));
        var bearer =
                mvc.perform(
                                get("/api/v1/clientes")
                                        .cookie(cookie)
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse();
        assertThat(bearer.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(mapper.readTree(bearer.getContentAsString()).get("itens").size()).isZero();
        verify(clientes).listar(0, 20);
        verifyNoMoreInteractions(clientes);
    }
}
