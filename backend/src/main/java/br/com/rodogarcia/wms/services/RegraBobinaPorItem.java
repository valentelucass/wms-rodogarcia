package br.com.rodogarcia.wms.services;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Regra operacional confirmada por cliente/produto, sem inferência pelo nome ou peso. */
@Component
public class RegraBobinaPorItem {
    private final Set<String> pares;

    public RegraBobinaPorItem(
            @Value("${wms.recebimento.xml.bobina-por-item:}") String configuracao) {
        pares =
                Arrays.stream(configuracao.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        if (pares.stream().anyMatch(s -> !s.matches("[A-Z0-9]{11,14}:[A-Za-z0-9._/-]+")))
            throw new IllegalArgumentException("Regra de bobinas inválida");
    }

    public boolean aplica(String emitente, String sku, String unidade) {
        return pares.contains(emitente + ":" + sku)
                && Set.of("KG", "TO", "T", "TON").contains(unidade);
    }
}
