package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.config.NotazzProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Transporte fixo, sem redirects, retries de escrita, download de URL ou logs de credenciais. */
@Service
public class NotazzGateway {
    private static final URI ENDPOINT = URI.create("https://app.notazz.com/api");
    private final HttpClient client;
    private final JsonMapper mapper;
    private final NotazzProperties properties;

    public NotazzGateway(
            @Qualifier("notazzHttpClient") HttpClient client,
            JsonMapper mapper,
            NotazzProperties properties) {
        this.client = client;
        this.mapper = mapper;
        this.properties = properties;
    }

    public JsonNode chamar(
            NotazzProperties.Emitente emitente, String metodo, Map<String, Object> dados) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("I/O fiscal exige transação local encerrada.");
        if (!properties.isEnabled()
                || emitente == null
                || emitente.getApiKey() == null
                || emitente.getApiKey().isBlank()
                || !emitente.isConfiguracaoConferida()
                || !properties.ambientePermitido(emitente))
            throw CadastroSupport.invalido(
                    "Integração Notazz não habilitada/conferida para este estabelecimento e ambiente.");
        if (!java.util.Set.of("create_nfe_55", "consult_nfe_55", "cancel_nfe_55").contains(metodo))
            throw new IllegalArgumentException("Método fiscal não permitido.");
        var body = new LinkedHashMap<String, Object>(dados);
        body.put("API_KEY", emitente.getApiKey());
        body.put("METHOD", metodo);
        var request =
                HttpRequest.newBuilder(ENDPOINT)
                        .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Accept", "application/json")
                        .POST(
                                HttpRequest.BodyPublishers.ofString(
                                        mapper.writeValueAsString(body), StandardCharsets.UTF_8))
                        .build();
        try {
            var response = client.send(request, info -> respostaLimitada());
            byte[] bytes = response.body();
            if (response.statusCode() != 200 || bytes.length > 4_000_000)
                throw new IOException("Resposta fiscal não confirmada.");
            var json = mapper.readTree(bytes);
            if (json == null || !json.isObject())
                throw new IOException("Resposta fiscal inválida.");
            return json;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResultadoDesconhecido();
        } catch (IOException | tools.jackson.core.JacksonException ex) {
            throw new ResultadoDesconhecido();
        }
    }

    /** O corpo conclui dentro do timeout HTTP; não devolve um stream que possa bloquear depois. */
    static HttpResponse.BodySubscriber<byte[]> respostaLimitada() {
        return new HttpResponse.BodySubscriber<byte[]>() {
            private final java.util.concurrent.CompletableFuture<byte[]> resultado =
                    new java.util.concurrent.CompletableFuture<>();
            private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            private java.util.concurrent.Flow.Subscription subscription;

            public java.util.concurrent.CompletionStage<byte[]> getBody() {
                return resultado;
            }

            public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                subscription = s;
                s.request(1);
            }

            public void onNext(java.util.List<java.nio.ByteBuffer> buffers) {
                for (var buffer : buffers) {
                    if (buffer.remaining() > 4_000_000 - bytes.size()) {
                        subscription.cancel();
                        resultado.completeExceptionally(
                                new IOException("Resposta fiscal excede o limite."));
                        return;
                    }
                    var chunk = new byte[buffer.remaining()];
                    buffer.get(chunk);
                    bytes.writeBytes(chunk);
                }
                subscription.request(1);
            }

            public void onError(Throwable ex) {
                resultado.completeExceptionally(ex);
            }

            public void onComplete() {
                resultado.complete(bytes.toByteArray());
            }
        };
    }

    public static final class ResultadoDesconhecido extends RuntimeException {
        public ResultadoDesconhecido() {
            super(
                    "Resultado Notazz ainda não confirmado. Consulte a solicitação; não emita outra nota.");
        }
    }
}
