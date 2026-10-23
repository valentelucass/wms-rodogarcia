package br.com.rodogarcia.wms.services;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Trabalho limitado por confirmação; autenticação capturada e revalidada em cada operação. */
@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class FiscalNfeEnvioService {
    public record Confirmada(Long pedidoId, List<String> solicitacoes) {}

    private final FiscalNfeService fiscal;

    public FiscalNfeEnvioService(FiscalNfeService fiscal) {
        this.fiscal = fiscal;
    }

    @Async("notazzExecutor")
    @EventListener
    public void enviar(Confirmada evento) {
        for (int rodada = 0; rodada < 3; rodada++)
            for (var id : evento.solicitacoes()) {
                try {
                    var retorno = fiscal.processar(evento.pedidoId(), UUID.fromString(id));
                    if (retorno.solicitacoes().stream()
                            .filter(s -> evento.solicitacoes().contains(s.id()))
                            .allMatch(
                                    s ->
                                            java.util.Set.of(
                                                            "AUTORIZADA",
                                                            "CANCELADA",
                                                            "REJEITADA",
                                                            "CONFLITO")
                                                    .contains(s.situacao()))) return;
                    Thread.sleep(750);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (RuntimeException ex) {
                    return;
                } // Solicitação durável permanece no acompanhamento, sem repetir criação
                // desconhecida.
            }
    }
}
