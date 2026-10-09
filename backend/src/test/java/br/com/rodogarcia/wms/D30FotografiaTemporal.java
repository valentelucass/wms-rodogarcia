package br.com.rodogarcia.wms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

/** Evidência observacional tipada. SQL TIMESTAMP sem zona interpretado explicitamente em UTC. */
final class D30FotografiaTemporal {
    private D30FotografiaTemporal() {}

    record Campo(
            String coluna,
            String tipoSQL,
            int codigoSQL,
            int precisao,
            int escala,
            String classeJDBCDeclarada,
            String classeJDBCObservada,
            boolean nulo,
            String local,
            String utc,
            Integer nanos,
            Integer micros,
            String calendario,
            String semantica) {}

    record Linha(long id, Map<String, Campo> colunas) {}

    static void exigirH2Proprio(Connection connection) throws SQLException {
        var md = connection.getMetaData();
        String url = md.getURL();
        if (!"H2".equals(md.getDatabaseProductName())
                || !(br.com.rodogarcia.wms.config.D30ContextoLocalGuard.urlCapturaLocalPermitida(
                                url)
                        || url.matches("jdbc:h2:mem:d30-cedro-temporal-[0-9a-f-]{36}"))) {
            throw new IllegalStateException(
                    "Captura D30 temporal exige H2memory próprio comprovado.");
        }
    }

    static Map<String, List<Linha>> capturar(JdbcTemplate jdbc) {
        return jdbc.execute(
                (ConnectionCallback<Map<String, List<Linha>>>)
                        connection -> {
                            exigirH2Proprio(connection);
                            List<String> tabelas = new ArrayList<>();
                            try (var statement = connection.createStatement();
                                    var rs =
                                            statement.executeQuery(
                                                    "select table_name from information_schema.tables where table_schema='WMS' and table_type='BASE TABLE' order by table_name")) {
                                while (rs.next()) tabelas.add(rs.getString(1));
                            }
                            var result = new LinkedHashMap<String, List<Linha>>();
                            for (String tabela : tabelas) {
                                if (!tabela.matches("[A-Z_]+"))
                                    throw new IllegalStateException("Tabela de captura inválida.");
                                List<Linha> rows = new ArrayList<>();
                                try (var statement = connection.createStatement();
                                        var rs =
                                                statement.executeQuery(
                                                        "select * from wms."
                                                                + tabela
                                                                + " order by id")) {
                                    while (rs.next()) rows.add(ler(rs));
                                }
                                result.put(tabela, List.copyOf(rows));
                            }
                            return result;
                        });
    }

    static Linha ler(ResultSet rs) throws SQLException {
        var md = rs.getMetaData();
        var campos = new LinkedHashMap<String, Campo>();
        for (int i = 1; i <= md.getColumnCount(); i++) {
            int tipo = md.getColumnType(i);
            if (!List.of(
                            Types.TIMESTAMP,
                            Types.TIMESTAMP_WITH_TIMEZONE,
                            Types.DATE,
                            Types.TIME,
                            Types.TIME_WITH_TIMEZONE)
                    .contains(tipo)) continue;
            String local = null, utc = null, calendario = null;
            Integer nanos = null, micros = null;
            Object bruto = rs.getObject(i);
            String semantica;
            if (tipo == Types.TIMESTAMP) {
                semantica =
                        "SQL_TIMESTAMP_SEM_ZONA;UTC_EXPLICITO_POR_CALENDAR;NAO_SERIALIZAR_TIMESTAMP_COMO_DATE";
                calendario = "UTC";
                LocalDateTime valor = rs.getObject(i, LocalDateTime.class);
                if (valor != null) {
                    var timestampUTC =
                            rs.getTimestamp(i, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
                    local = valor.toString();
                    utc = timestampUTC.toInstant().toString();
                    nanos = valor.getNano();
                    micros = nanos / 1000;
                    if (timestampUTC.getNanos() != nanos
                            || !valor.toInstant(java.time.ZoneOffset.UTC)
                                    .equals(timestampUTC.toInstant()))
                        throw new IllegalStateException("Leituras temporais tipadas D30 divergem.");
                }
            } else if (tipo == Types.TIMESTAMP_WITH_TIMEZONE) {
                semantica = "SQL_TIMESTAMP_COM_OFFSET;UTC_POR_INSTANT";
                OffsetDateTime valor = rs.getObject(i, OffsetDateTime.class);
                if (valor != null) {
                    local = valor.toString();
                    utc = valor.toInstant().toString();
                    nanos = valor.getNano();
                    micros = nanos / 1000;
                }
            } else if (tipo == Types.DATE) {
                semantica = "SQL_DATE;DATA_CIVIL_SEM_INSTANT_OU_ZONA";
                LocalDate valor = rs.getObject(i, LocalDate.class);
                if (valor != null) local = valor.toString();
            } else if (tipo == Types.TIME) {
                semantica = "SQL_TIME;HORA_CIVIL_SEM_DATA_OU_ZONA";
                LocalTime valor = rs.getObject(i, LocalTime.class);
                if (valor != null) {
                    local = valor.toString();
                    nanos = valor.getNano();
                    micros = nanos / 1000;
                }
            } else {
                semantica = "SQL_TIME_COM_OFFSET;SEM_DATA_NAO_HA_INSTANT";
                OffsetTime valor = rs.getObject(i, OffsetTime.class);
                if (valor != null) {
                    local = valor.toString();
                    nanos = valor.getNano();
                    micros = nanos / 1000;
                }
            }
            String coluna = md.getColumnLabel(i);
            campos.put(
                    coluna,
                    new Campo(
                            coluna,
                            md.getColumnTypeName(i),
                            tipo,
                            md.getPrecision(i),
                            md.getScale(i),
                            md.getColumnClassName(i),
                            bruto == null ? null : bruto.getClass().getName(),
                            bruto == null,
                            local,
                            utc,
                            nanos,
                            micros,
                            calendario,
                            semantica));
        }
        return new Linha(rs.getLong("ID"), Map.copyOf(campos));
    }
}
