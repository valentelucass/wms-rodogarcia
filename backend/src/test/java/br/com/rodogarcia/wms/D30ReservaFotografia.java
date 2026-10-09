package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Complementa os fluxos existentes com marcos JDBC tipados; somente H2 isolado. */
final class D30ReservaFotografia {
    private D30ReservaFotografia() {}

    record Linha(
            long id,
            Map<String, Object> origem,
            String situacao,
            D30FotografiaTemporal.Campo criada,
            D30FotografiaTemporal.Campo encerrada) {}

    static List<Linha> capturar(JdbcTemplate jdbc, long pedido) {
        return jdbc.query(
                "select * from wms.reserva_saida where pedido_id=? order by id",
                (rs, indice) -> {
                    D30FotografiaTemporal.exigirH2Proprio(rs.getStatement().getConnection());
                    var temporal = D30FotografiaTemporal.ler(rs).colunas();
                    var origem = new LinkedHashMap<String, Object>();
                    var md = rs.getMetaData();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        String nome = md.getColumnLabel(i);
                        if (!List.of("SITUACAO", "CRIADA_EM", "ENCERRADA_EM").contains(nome))
                            origem.put(nome, rs.getObject(i));
                    }
                    return new Linha(
                            rs.getLong("ID"),
                            origem,
                            rs.getString("SITUACAO"),
                            temporal.get("CRIADA_EM"),
                            temporal.get("ENCERRADA_EM"));
                },
                pedido);
    }

    static void exigirAtivas(List<Linha> linhas) {
        assertThat(linhas).isNotEmpty();
        for (var l : linhas) {
            assertThat(l.situacao()).isEqualTo("ATIVA");
            assertThat(l.criada().nulo()).isFalse();
            assertThat(l.criada().calendario()).isEqualTo("UTC");
            assertThat(l.encerrada().nulo()).isTrue();
        }
    }

    static void exigirEncerradas(
            List<Linha> antes, List<Linha> depois, String fim, Instant inicio, Instant termino) {
        assertThat(depois).hasSameSizeAs(antes);
        for (int i = 0; i < antes.size(); i++) {
            var a = antes.get(i);
            var d = depois.get(i);
            assertThat(d.id()).isEqualTo(a.id());
            assertThat(d.origem()).isEqualTo(a.origem());
            assertThat(d.criada()).isEqualTo(a.criada());
            assertThat(d.situacao()).isEqualTo(fim);
            assertThat(d.encerrada().nulo()).isFalse();
            assertThat(d.encerrada().calendario()).isEqualTo("UTC");
            assertThat(d.encerrada().nanos() % 1000).isZero();
            assertThat(Instant.parse(d.encerrada().utc()))
                    .isBetween(
                            inicio.truncatedTo(ChronoUnit.MICROS),
                            termino.truncatedTo(ChronoUnit.MICROS));
        }
    }
}
