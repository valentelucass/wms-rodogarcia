package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiHttpIntegrationTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Autowired private Environment environment;

    @Autowired private JsonMapper mapper;

    @Test
    void disponibilizaStatusPeloServidorHttpSemExporConfiguracao() throws Exception {
        var response = enviar("GET", "/api/v1/status");
        var json = mapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(json.get("aplicacao").asString()).isEqualTo("wms-rodogarcia");
        assertThat(json.get("status").asString()).isEqualTo("DISPONIVEL");
        assertThat(Instant.parse(json.get("instante").asString())).isBeforeOrEqualTo(Instant.now());
        assertThat(json.size()).isEqualTo(3);
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
        assertThat(UUID.fromString(response.headers().firstValue("X-Request-Id").orElseThrow()))
                .isNotNull();
    }

    @Test
    void negaRotasDeNegocioSemGerarLoginOuSessao() throws Exception {
        var response = enviar("GET", "/api/v1/clientes?senha=segredo-ficticio");
        var json = mapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Content-Type"))
                .contains("application/problem+json");
        assertThat(json.get("codigo").asString()).isEqualTo("ACESSO_NEGADO");
        assertThat(json.get("idOperacao").asString())
                .isEqualTo(response.headers().firstValue("X-Request-Id").orElseThrow());
        assertThat(response.body())
                .doesNotContain("senha", "segredo-ficticio", "exception", "trace");
        assertThat(response.headers().firstValue("WWW-Authenticate")).isEmpty();
        assertThat(response.headers().firstValue("Set-Cookie")).isEmpty();
    }

    @Test
    void negaEscritaMesmoNaRotaPublica() throws Exception {
        var response = enviar("POST", "/api/v1/status");
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(mapper.readTree(response.body()).get("codigo").asString())
                .isEqualTo("ACESSO_NEGADO");
    }

    @Test
    void geraIdentificadorProprioENovoParaCadaRequisicao() throws Exception {
        var request =
                HttpRequest.newBuilder(url("/api/v1/status"))
                        .header("X-Request-Id", "identificador-forjado")
                        .GET()
                        .build();
        var primeira = client.send(request, HttpResponse.BodyHandlers.ofString());
        var segunda = client.send(request, HttpResponse.BodyHandlers.ofString());
        String id = primeira.headers().firstValue("X-Request-Id").orElseThrow();

        assertThat(UUID.fromString(id)).isNotNull();
        assertThat(id)
                .isNotEqualTo("identificador-forjado")
                .isNotEqualTo(segunda.headers().firstValue("X-Request-Id").orElseThrow());
    }

    private HttpResponse<String> enviar(String metodo, String caminho) throws Exception {
        return client.send(
                HttpRequest.newBuilder(url(caminho))
                        .method(metodo, HttpRequest.BodyPublishers.noBody())
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String caminho) {
        return URI.create(
                "http://127.0.0.1:"
                        + environment.getRequiredProperty("local.server.port")
                        + caminho);
    }
}
