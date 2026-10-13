package br.com.rodogarcia.wms.services;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Correspondência opcional; nunca altera o SKU cadastrado ou o XML original. */
@Component
public class RegraSkuXml {
    private final Set<String> clientes;

    public RegraSkuXml(
            @Value("${wms.recebimento.xml.sku-sem-zeros-clientes:}") String configuracao) {
        clientes =
                Arrays.stream(configuracao.split(","))
                        .map(String::strip)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
        if (clientes.stream().anyMatch(s -> !s.matches("[0-9]{11}|[0-9]{14}")))
            throw new IllegalArgumentException("Configuração de correspondência SKU inválida");
    }

    public boolean habilitada(String documento) {
        return clientes.contains(documento);
    }

    public List<String> variantes(String documento, String sku) {
        if (!habilitada(documento) || !sku.matches("[0-9]+")) return List.of(sku);
        String base = sku.replaceFirst("^0+", "");
        // Sem algarismo diferente de zero não há referência para a regra.
        if (base.isEmpty() || base.length() > 40) return List.of(sku);
        // Limite do cadastro: no máximo 40 buscas indexáveis, sem conversão numérica.
        return IntStream.rangeClosed(base.length(), 40)
                .mapToObj(length -> "0".repeat(length - base.length()) + base)
                .toList();
    }
}
