package br.com.rodogarcia.wms.config;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.resource.jdbc.spi.StatementInspector;

/** Contador por thread apenas no contexto H2 de visão; não registra valores vinculados. */
public class Perf01StatementInspector implements StatementInspector {
    private static final ThreadLocal<List<String>> SQL = new ThreadLocal<>();

    public static void iniciar() {
        SQL.set(new ArrayList<>());
    }

    public static List<String> terminar() {
        var queries = SQL.get();
        SQL.remove();
        return List.copyOf(queries);
    }

    @Override
    public String inspect(String sql) {
        var queries = SQL.get();
        if (queries != null) queries.add(sql);
        return sql;
    }
}
