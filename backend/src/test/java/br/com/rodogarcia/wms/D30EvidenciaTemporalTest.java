package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** A08: H2memory próprio; primeira execução prevista na regressão FINAL, sem SQL Server. */
class D30EvidenciaTemporalTest {
    @Test
    void capturaUTCNanosMicrosNullETipoSemSerializacaoDate() throws Exception {
        String url = "jdbc:h2:mem:d30-cedro-temporal-" + UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            D30FotografiaTemporal.exigirH2Proprio(connection);
            try (var s = connection.createStatement()) {
                s.execute(
                        "create table d30_temporal(id int primary key, fifo timestamp(6), chegada timestamp(6), primeiro timestamp(6), inicio timestamp(6))");
            }
            try (var insert =
                    connection.prepareStatement(
                            "insert into d30_temporal(id,fifo,chegada) values(1,?,?)")) {
                var utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                insert.setTimestamp(
                        1, Timestamp.from(Instant.parse("2026-09-01T12:00:00.123456Z")), utc);
                insert.setTimestamp(
                        2, Timestamp.from(Instant.parse("2026-09-02T12:00:00.234567Z")), utc);
                assertThat(insert.executeUpdate()).isEqualTo(1);
            }
            try (var s = connection.createStatement();
                    var rs = s.executeQuery("select * from d30_temporal where id=1")) {
                assertThat(rs.next()).isTrue();
                var linha = D30FotografiaTemporal.ler(rs);
                assertThat(linha.colunas().get("FIFO").utc())
                        .isEqualTo("2026-09-01T12:00:00.123456Z");
                assertThat(linha.colunas().get("FIFO").local())
                        .isEqualTo("2026-09-01T12:00:00.123456");
                assertThat(linha.colunas().get("FIFO").nanos()).isEqualTo(123456000);
                assertThat(linha.colunas().get("FIFO").micros()).isEqualTo(123456);
                assertThat(linha.colunas().get("FIFO").escala()).isEqualTo(6);
                assertThat(linha.colunas().get("FIFO").classeJDBCObservada())
                        .isEqualTo("java.sql.Timestamp");
                assertThat(linha.colunas().get("CHEGADA").utc())
                        .isEqualTo("2026-09-02T12:00:00.234567Z");
                assertThat(linha.colunas().get("CHEGADA").nanos()).isEqualTo(234567000);
                assertThat(linha.colunas().get("PRIMEIRO").nulo()).isTrue();
                assertThat(linha.colunas().get("INICIO").nulo()).isTrue();
                assertThat(linha.colunas().get("PRIMEIRO").utc()).isNull();
                var mapper = JsonMapper.builder().build();
                var json = mapper.readTree(mapper.writeValueAsString(linha));
                assertThat(json.get("colunas").get("FIFO").get("utc").asString())
                        .isEqualTo("2026-09-01T12:00:00.123456Z");
                assertThat(json.get("colunas").get("FIFO").get("nanos").intValue())
                        .isEqualTo(123456000);
                assertThat(json.get("colunas").get("CHEGADA").get("micros").intValue())
                        .isEqualTo(234567);
                assertThat(json.get("colunas").get("PRIMEIRO").get("nulo").booleanValue()).isTrue();
                assertThat(
                                mapper.readValue(
                                        mapper.writeValueAsString(linha),
                                        D30FotografiaTemporal.Linha.class))
                        .isEqualTo(linha);
            }
        }
    }
}
