package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.CadastrosSegurancaConfig;
import br.com.rodogarcia.wms.config.IdentidadeTesteConfig;
import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.controllers.ClienteController;
import br.com.rodogarcia.wms.dto.PaginaResponse;
import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import br.com.rodogarcia.wms.services.ClienteService;
import jakarta.persistence.EntityManagerFactory;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.LongStream;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

/** TCP loopback efemero/Tomcat real/JWT RSA efemero; nenhum datasource ou provedor. */
@ActiveProfiles("test")
@TestPropertySource("classpath:d30-cedro-security-test.properties")
@Import(IdentidadeTesteConfig.class)
@SpringBootTest(
        classes = D30JwtHttpHeaderTest.Contexto.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class D30JwtHttpHeaderTest {
    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @Import({CadastrosSegurancaConfig.class, ClienteController.class})
    static class Contexto {
        @Bean
        ClienteService clientes() {
            return mock(ClienteService.class);
        }

        @Bean
        ProblemasApi problemas(JsonMapper mapper) {
            return new ProblemasApi(mapper);
        }

        @Bean
        IdentificacaoOperacaoFilter identificacao() {
            return new IdentificacaoOperacaoFilter();
        }
    }

    @Autowired ServletWebServerApplicationContext contexto;
    @Autowired Environment env;
    @Autowired ServerProperties properties;
    @Autowired JwtEncoder encoder;
    @Autowired JwtDecoder decoder;
    @Autowired ClienteService clientes;
    @Autowired JsonMapper mapper;

    @Test
    void tokenComLimitesDeDominioAtravessaHeaderHttpRealSemBancoOuSessao() throws Exception {
        assertThat(contexto.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(contexto.getBeansOfType(EntityManagerFactory.class)).isEmpty();
        assertThat(env.getActiveProfiles()).containsExactly("test");
        assertThat(env.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(env.getProperty("spring.datasource.url")).isNull();
        var ids =
                LongStream.rangeClosed(Long.MAX_VALUE - 499, Long.MAX_VALUE)
                        .mapToObj(Long::toString)
                        .toList();
        assertThat(ids).hasSize(500).doesNotHaveDuplicates();
        assertThat(ids.getLast()).isEqualTo("9223372036854775807");
        Instant inicio = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer("https://identidade.test.invalid")
                        .audience(List.of("wms-testes"))
                        .subject("S".repeat(200))
                        .issuedAt(inicio)
                        .expiresAt(inicio.plusSeconds(900))
                        .claim("wms_perfil", "SUPERVISOR")
                        .claim("wms_clientes", ids)
                        .claim("wms_armazens", ids)
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(SignatureAlgorithm.RS256).build(), claims))
                        .getTokenValue();
        var jwt = decoder.decode(token);
        assertThat(jwt.getSubject()).hasSize(200);
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()))
                .isEqualTo(Duration.ofSeconds(900));
        assertThat(jwt.getClaimAsStringList("wms_clientes")).containsExactlyElementsOf(ids);
        assertThat(jwt.getClaimAsStringList("wms_armazens")).containsExactlyElementsOf(ids);
        when(clientes.listar(0, 20)).thenReturn(new PaginaResponse<>(List.of(), 0, 20, 0, 0));
        var server = (TomcatWebServer) contexto.getWebServer();
        var connector = server.getTomcat().getConnector();
        var proof = new LinkedHashMap<String, Object>();
        proof.put("caso", "D30-C-CASO-JWT-HTTP-HEADER-001");
        proof.put("serverPropertiesBytes", properties.getMaxHttpRequestHeaderSize().toBytes());
        proof.put(
                "connectorMaxHttpRequestHeaderSize",
                connector.getProperty("maxHttpRequestHeaderSize"));
        proof.put("tokenBytesASCII", token.getBytes(StandardCharsets.US_ASCII).length);
        proof.put("clientesStrings", 500);
        proof.put("armazensStrings", 500);
        proof.put("subCaracteres", 200);
        proof.put("duracaoSegundos", 900);
        proof.put("dataSources", 0);
        proof.put("entityManagerFactories", 0);
        proof.put("SQLServerAcoes", 0);
        proof.put("host", "127.0.0.1");
        int porta = server.getPort();
        assertThat(porta).isPositive();
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var response =
                http.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                "http://127.0.0.1:" + porta + "/api/v1/clientes"))
                                .timeout(Duration.ofSeconds(15))
                                .header("Authorization", "Bearer " + token)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        proof.put("HTTPstatus", response.statusCode());
        proof.put("chamadasNegocioObservadas", mockingDetails(clientes).getInvocations().size());
        Path dir =
                Path.of(System.getProperty("wms.test.evidencias.dir")).toAbsolutePath().normalize();
        assertThat(dir.toString()).contains("d30-cedro-");
        Files.createDirectories(dir);
        Files.writeString(
                dir.resolve("d30-cedro-jwt-header-resultado.json"),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proof),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        assertThat(response.statusCode()).isEqualTo(200);
        var json = mapper.readTree(response.body());
        assertThat(json.get("pagina").intValue()).isZero();
        assertThat(json.get("tamanho").intValue()).isEqualTo(20);
        assertThat(json.get("itens").size()).isZero();
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
        verify(clientes).listar(0, 20);
        verifyNoMoreInteractions(clientes);
        assertThat(properties.getMaxHttpRequestHeaderSize().toBytes()).isEqualTo(65536);
        var excedido =
                http.send(
                        HttpRequest.newBuilder(
                                        URI.create(
                                                "http://127.0.0.1:" + porta + "/api/v1/clientes"))
                                .timeout(Duration.ofSeconds(15))
                                .header("Authorization", "Bearer " + token)
                                .header("X-D30-Limite", "X".repeat(131072))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
        assertThat(excedido.statusCode()).isEqualTo(400);
        verifyNoMoreInteractions(clientes);
        Files.writeString(
                dir.resolve("d30-cedro-jwt-header-excedido.json"),
                mapper.writeValueAsString(
                        java.util.Map.of(
                                "tetoEfetivoBytes", 65536,
                                "headerExtraASCIIBytes", 131072,
                                "HTTPstatus", excedido.statusCode(),
                                "chamadasNegocioTotalAposRecusa",
                                        mockingDetails(clientes).getInvocations().size(),
                                "efeitosNegocioExtras", 0,
                                "SQLServerAcoes", 0)),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
    }
}
