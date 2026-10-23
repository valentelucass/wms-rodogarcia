package br.com.rodogarcia.wms.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.config.NotazzProperties;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

class NotazzGatewayTest {
    final HttpClient client = mock(HttpClient.class);
    final NotazzProperties properties = new NotazzProperties();
    final NotazzProperties.Emitente issuer = new NotazzProperties.Emitente();

    NotazzGatewayTest() {
        properties.setEnabled(true);
        issuer.setApiKey("ficticio-nunca-enviar");
        issuer.setCnpj(FiscalNfeTestFixture.CNPJ);
        issuer.setApiKeySha256Conferida(NfeXmlService.hash("ficticio-nunca-enviar"));
        properties.getCnpjsHomologacaoPermitidos().add(FiscalNfeTestFixture.CNPJ);
        issuer.setAmbiente(2);
        issuer.setConfiguracaoConferida(true);
    }

    NotazzGateway gateway() {
        return new NotazzGateway(client, new JsonMapper(), properties);
    }

    @SuppressWarnings("unchecked")
    @Test
    void postUsaDestinoFixoChaveProtegidaEJsonSemRetry() throws Exception {
        HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body())
                .thenReturn(
                        "{\"statusProcessamento\":\"sucesso\",\"codigoProcessamento\":\"000\",\"id\":\"exemplo\"}"
                                .getBytes(StandardCharsets.UTF_8));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        assertThat(
                        gateway()
                                .chamar(
                                        issuer,
                                        "create_nfe_55",
                                        Map.of(
                                                "TRANSMITIR",
                                                "1",
                                                "EXTERNAL_ID",
                                                FiscalNfeTestFixture.ID))
                                .path("id")
                                .asString())
                .isEqualTo("exemplo");
        var captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captor.capture(), any(HttpResponse.BodyHandler.class));
        var r = captor.getValue();
        assertThat(r.uri().toString()).isEqualTo("https://app.notazz.com/api");
        assertThat(r.method()).isEqualTo("POST");
        assertThat(r.timeout()).contains(java.time.Duration.ofSeconds(12));
        var content = new java.io.ByteArrayOutputStream();
        r.bodyPublisher()
                .orElseThrow()
                .subscribe(
                        new Flow.Subscriber<ByteBuffer>() {
                            public void onSubscribe(Flow.Subscription s) {
                                s.request(Long.MAX_VALUE);
                            }

                            public void onNext(ByteBuffer item) {
                                byte[] b = new byte[item.remaining()];
                                item.get(b);
                                content.writeBytes(b);
                            }

                            public void onError(Throwable error) {
                                throw new AssertionError(error);
                            }

                            public void onComplete() {}
                        });
        var body = new JsonMapper().readTree(content.toByteArray());
        assertThat(body.path("API_KEY").asString()).isEqualTo("ficticio-nunca-enviar");
        assertThat(body.path("METHOD").asString()).isEqualTo("create_nfe_55");
        assertThat(body.path("EXTERNAL_ID").asString()).isEqualTo(FiscalNfeTestFixture.ID);
    }

    @Test
    void corpoExcessivoCancelaRecepcaoAntesDeAlocarRespostaInteira() {
        var body = NotazzGateway.respostaLimitada();
        var subscription = mock(Flow.Subscription.class);
        body.onSubscribe(subscription);
        body.onNext(java.util.List.of(ByteBuffer.allocate(4_000_001)));
        assertThatThrownBy(() -> body.getBody().toCompletableFuture().join())
                .hasCauseInstanceOf(java.io.IOException.class);
        verify(subscription).cancel();
    }

    @Test
    void producaoSemHabilitacaoExplicitaNaoAbreTransporte() {
        issuer.setAmbiente(1);
        assertThatThrownBy(() -> gateway().chamar(issuer, "create_nfe_55", Map.of()))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(client);
    }

    @Test
    void chaveDiferenteDaConferidaNaoAbreTransporteMesmoComCnpjDeHomologacao() {
        issuer.setApiKey("outra-chave-ficticia");
        assertThatThrownBy(() -> gateway().chamar(issuer, "create_nfe_55", Map.of()))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(client);
    }

    @Test
    void castroEAgudosNaoPodemSerDisfarcadosComoHomologacao() {
        for (var cnpj : java.util.List.of("04547874000203", "04547874000386")) {
            issuer.setCnpj(cnpj);
            issuer.setAmbiente(2);
            assertThatThrownBy(() -> gateway().chamar(issuer, "create_nfe_55", Map.of()))
                    .isInstanceOf(RuntimeException.class);
        }
        verifyNoInteractions(client);
    }

    @Test
    void integracaoDesligadaOuMetodoForaDoEscopoNaoAbreTransporte() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> gateway().chamar(issuer, "create_nfe_55", Map.of()))
                .isInstanceOf(RuntimeException.class);
        properties.setEnabled(true);
        assertThatThrownBy(() -> gateway().chamar(issuer, "update_stock", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @Test
    void naoMantemTransacaoDeEstoqueDuranteIo() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThatThrownBy(() -> gateway().chamar(issuer, "create_nfe_55", Map.of()))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            TransactionSynchronizationManager.clear();
        }
        verifyNoInteractions(client);
    }

    @SuppressWarnings("unchecked")
    @Test
    void falhaDeRedePermaneceDesconhecidaSemPublicarChave() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("ficticio-nunca-enviar"));
        assertThatThrownBy(() -> gateway().chamar(issuer, "create_nfe_55", Map.of()))
                .isInstanceOf(NotazzGateway.ResultadoDesconhecido.class)
                .hasMessageNotContaining("ficticio-nunca-enviar");
        verify(client).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}
