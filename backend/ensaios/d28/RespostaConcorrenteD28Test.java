import java.net.URI;
import java.net.http.*;
import java.util.*;

import javax.net.ssl.SSLSession;

/** Oráculo sintético offline; não é chamada HTTP ao JAR nem prova SQL. */
public class RespostaConcorrenteD28Test {
    record Response(int statusCode, String body, HttpHeaders headers)
            implements HttpResponse<String> {
        public HttpRequest request() {
            return HttpRequest.newBuilder(URI.create("http://127.0.0.1/fixture")).build();
        }

        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        public URI uri() {
            return request().uri();
        }

        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    public static void main(String[] args) throws Exception {
        var x = new EnsaioD28("D28-CREDENCIAL-FICTICIA-OFFLINE");
        x.phase = "offline-controles-concorrentes";
        var good = HttpHeaders.of(Map.of("X-Request-Id", List.of("fixture")), (k, v) -> true);
        for (int code : List.of(200, 409))
            x.recordResponse(
                    "controle sintetico status permitido",
                    "POST",
                    "/fixture",
                    Map.of(),
                    "OPERACAO",
                    200,
                    new Response(code, "{}", good),
                    true);
        boolean cookie = false, header = false, leak = false;
        try {
            x.recordResponse(
                    "controle sintetico cookie recusado",
                    "POST",
                    "/fixture",
                    Map.of(),
                    "OPERACAO",
                    200,
                    new Response(
                            200,
                            "{}",
                            HttpHeaders.of(
                                    Map.of(
                                            "X-Request-Id",
                                            List.of("fixture"),
                                            "Set-Cookie",
                                            List.of("D28-COOKIE-FICTICIO-NAO-SERIALIZAR")),
                                    (k, v) -> true)),
                    true);
        } catch (EnsaioD28.CaseFailure e) {
            cookie = true;
        }
        try {
            x.recordResponse(
                    "controle sintetico request-id ausente",
                    "POST",
                    "/fixture",
                    Map.of(),
                    "OPERACAO",
                    200,
                    new Response(409, "{}", HttpHeaders.of(Map.of(), (k, v) -> true)),
                    true);
        } catch (EnsaioD28.CaseFailure e) {
            header = true;
        }
        try {
            x.recordResponse(
                    "controle sintetico segredo recusado",
                    "POST",
                    "/fixture",
                    Map.of(),
                    "OPERACAO",
                    200,
                    new Response(200, "{\"campo\":\"" + x.secret + "\"}", good),
                    true);
        } catch (EnsaioD28.CaseFailure e) {
            leak = true;
        }
        var doc =
                java.nio.file.Files.readString(x.evidence.resolve("d28-" + x.round + "-http.json"));
        boolean pass =
                cookie
                        && header
                        && leak
                        && !doc.contains(x.secret)
                        && !doc.contains("D28-COOKIE-FICTICIO-NAO-SERIALIZAR")
                        && x.cases.get(4).get("resposta").equals(EnsaioD28.m("omitida", true));
        System.out.println(
                "{\"caso\":\"controles-concorrentes-offline\",\"rodada\":\""
                        + x.round
                        + "\",\"respostasSinteticas\":5,\"SQL\":false,\"cookiesValoresRegistrados\":false,\"aprovado\":"
                        + pass
                        + "}");
        if (!pass) throw new IllegalStateException("D28_CONTROLES_REGRESSAO_FALHOU");
    }
}
