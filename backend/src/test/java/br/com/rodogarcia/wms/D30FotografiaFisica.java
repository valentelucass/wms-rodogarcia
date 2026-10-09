package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Fotos completas dos complementos D30; temporais tipados substituem Date/Timestamp no JSON. */
final class D30FotografiaFisica {
    private D30FotografiaFisica() {}

    static Map<String, List<Map<String, Object>>> capturar(JdbcTemplate jdbc) {
        var temporais = D30FotografiaTemporal.capturar(jdbc);
        assertThat(temporais).hasSize(64);
        var result = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (var entry : temporais.entrySet()) {
            String tabela = entry.getKey();
            if (!tabela.matches("[A-Z_]+")) throw new IllegalStateException("Tabela D30 invalida");
            var linhas = jdbc.queryForList("select * from wms." + tabela + " order by id");
            assertThat(linhas).hasSize(entry.getValue().size());
            for (int i = 0; i < linhas.size(); i++) {
                var temporal = entry.getValue().get(i);
                assertThat(((Number) linhas.get(i).get("ID")).longValue()).isEqualTo(temporal.id());
                linhas.get(i).putAll(temporal.colunas());
            }
            result.put(tabela, linhas);
        }
        return result;
    }
}
