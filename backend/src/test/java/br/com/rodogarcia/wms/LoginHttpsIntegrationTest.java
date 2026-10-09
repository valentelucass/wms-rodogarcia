package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

/** HTTP interno de proxy HTTPS, com dados ficticios em H2; nunca SQL Server. */
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:wms-login-https;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS wms",
            "wms.auth.enabled=true",
            "wms.auth.origin=",
            "wms.auth.proxy-origin=http://localhost:59999",
            "wms.auth.secure-cookie=true",
            "server.address=127.0.0.1"
        })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginHttpsIntegrationTest {
    @Autowired Environment environment;
    @Autowired JsonMapper mapper;
    private final HttpClient http = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void chaves(DynamicPropertyRegistry registry) throws Exception {
        LoginIntegrationTest.chaves(registry);
    }

    @Test
    void proxyHttpsMantemCsrfCookiesSegurosLoginERenovacao() throws Exception {
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:wms-login-https");
        var csrfResponse =
                http.send(request("/csrf").GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(csrfResponse.statusCode()).isEqualTo(200);
        String csrfCookie = cookie(csrfResponse, "XSRF-TOKEN");
        var csrf = mapper.readTree(csrfResponse.body());
        var login =
                post(
                        "/entrar",
                        "https://primeiro.brs.devtunnels.ms",
                        csrfCookie,
                        csrf.path("header").asString(),
                        csrf.path("token").asString(),
                        Map.of(
                                "email",
                                "desenvolvedor@rodogarcia.com.br",
                                "senha",
                                "Fixture-temporaria-123!"));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(login.body()).path("usuario").path("trocarSenha").asBoolean())
                .isTrue();
        String refreshCookie = cookie(login, "WMS_REFRESH");
        String cookies = csrfCookie + "; " + refreshCookie;
        var foreign =
                post(
                        "/renovar",
                        "https://outro-test.invalid",
                        cookies,
                        csrf.path("header").asString(),
                        csrf.path("token").asString(),
                        Map.of());
        assertThat(foreign.statusCode()).isEqualTo(403);
        assertThat(mapper.readTree(foreign.body()).path("codigo").asString())
                .isEqualTo("ORIGEM_INVALIDA");
        var noCsrf =
                post(
                        "/renovar",
                        "https://primeiro.brs.devtunnels.ms",
                        cookies,
                        "X-XSRF-TOKEN",
                        "invalido",
                        Map.of());
        assertThat(noCsrf.statusCode()).isEqualTo(403);
        var renewed =
                post(
                        "/renovar",
                        "https://segundo.brs.devtunnels.ms",
                        cookies,
                        csrf.path("header").asString(),
                        csrf.path("token").asString(),
                        Map.of());
        assertThat(renewed.statusCode()).isEqualTo(200);
        assertThat(cookie(renewed, "WMS_REFRESH")).isNotEqualTo(refreshCookie);
        assertThat(mapper.readTree(renewed.body()).path("usuario").path("trocarSenha").asBoolean())
                .isTrue();
        String access = mapper.readTree(renewed.body()).path("accessToken").asString();
        var repeated =
                post(
                        "/senha",
                        "https://segundo.brs.devtunnels.ms",
                        cookies,
                        csrf.path("header").asString(),
                        csrf.path("token").asString(),
                        Map.of(
                                "senhaAtual",
                                "Fixture-temporaria-123!",
                                "novaSenha",
                                "Fixture-temporaria-123!"),
                        access);
        assertThat(repeated.statusCode()).isEqualTo(400);
        assertThat(mapper.readTree(repeated.body()).path("codigo").asString())
                .isEqualTo("SENHA_REPETIDA");
        var changed =
                post(
                        "/senha",
                        "https://segundo.brs.devtunnels.ms",
                        cookies,
                        csrf.path("header").asString(),
                        csrf.path("token").asString(),
                        Map.of(
                                "senhaAtual",
                                "Fixture-temporaria-123!",
                                "novaSenha",
                                "Fixture-nova-456!"),
                        access);
        assertThat(changed.statusCode()).isEqualTo(204);
        var loginNew =
                post(
                        "/entrar",
                        "https://terceiro.brs.devtunnels.ms",
                        csrfCookie,
                        csrf.path("header").asString(),
                        csrf.path("token").asString(),
                        Map.of(
                                "email",
                                "desenvolvedor@rodogarcia.com.br",
                                "senha",
                                "Fixture-nova-456!"));
        assertThat(loginNew.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(loginNew.body()).path("usuario").path("trocarSenha").asBoolean())
                .isFalse();
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(
                URI.create(
                        "http://127.0.0.1:"
                                + environment.getProperty("local.server.port")
                                + "/api/auth"
                                + path));
    }

    private HttpResponse<String> post(
            String path,
            String origin,
            String cookies,
            String header,
            String token,
            Map<String, ?> body,
            String... bearer)
            throws Exception {
        var builder =
                request(path)
                        .header("Origin", origin)
                        .header("X-Forwarded-Host", URI.create(origin).getRawAuthority())
                        .header("X-Forwarded-Proto", "https")
                        .header("Cookie", cookies)
                        .header(header, token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        if (bearer.length > 0) builder.header("Authorization", "Bearer " + bearer[0]);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String cookie(HttpResponse<String> response, String name) {
        String cookie =
                response.headers().allValues("set-cookie").stream()
                        .filter(value -> value.startsWith(name + "="))
                        .findFirst()
                        .orElseThrow();
        assertThat(cookie).contains("Secure", "HttpOnly", "SameSite=Strict", "Path=/api/auth");
        return cookie.substring(0, cookie.indexOf(';'));
    }
}
