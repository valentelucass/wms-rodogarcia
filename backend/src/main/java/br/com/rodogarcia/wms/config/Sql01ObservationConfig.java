package br.com.rodogarcia.wms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.json.JsonMapper;

/** Observacao optativa SQL01: nao abre conexoes, altera SQL ou publica endpoint. */
@Configuration(proxyBeanMethods = false)
@Profile("sqlserver-dev")
@ConditionalOnProperty(name = "wms.sql01.observation.enabled", havingValue = "true")
public class Sql01ObservationConfig {
    private final Path output;
    private final JsonMapper mapper;

    public Sql01ObservationConfig(Environment environment, JsonMapper mapper) throws IOException {
        output = validarDiretorio(environment);
        this.mapper = mapper;
    }

    static Path validarDiretorio(Environment environment) throws IOException {
        String repository = environment.getProperty("wms.sql01.repository-root", "");
        String directory = environment.getProperty("wms.sql01.observation.dir", "");
        if (repository.isBlank() || directory.isBlank()) {
            throw new IllegalStateException("SQL01 exige raiz e saida exclusiva explicitas.");
        }
        Path root = Path.of(repository).toRealPath();
        Path allowed = root.resolve("orchestracao/.runtime/qual-conf01/sql01/cedro").toRealPath();
        Path candidate = Path.of(directory).toRealPath();
        if (!candidate.startsWith(allowed)
                || candidate.equals(allowed)
                || !Files.isDirectory(candidate)) {
            throw new IllegalStateException("SQL01 recusa saida fora da posse exclusiva.");
        }
        return candidate;
    }

    @Bean
    Observador sql01Observador() {
        return new Observador(output, mapper);
    }

    @Bean
    ApplicationRunner sql01JdbcObservation(DataSource dataSource, Environment environment) {
        var observation = new Sql01JdbcObservation(output, mapper, environment);
        return args -> observation.registrar(dataSource);
    }

    @Bean
    HibernatePropertiesCustomizer sql01HibernateCustomizer(Observador observer) {
        return properties -> {
            if (properties.containsKey("hibernate.session_factory.statement_inspector")) {
                throw new IllegalStateException("SQL01 recusa substituir inspector preexistente.");
            }
            properties.put(
                    "hibernate.session_factory.statement_inspector",
                    (org.hibernate.resource.jdbc.spi.StatementInspector) observer::inspect);
        };
    }

    @Bean
    FilterRegistrationBean<Filtro> sql01ObservationFilter(Observador observer) {
        var registration = new FilterRegistrationBean<>(new Filtro(observer));
        // Depois da identificacao gerada no servidor e antes da cadeia de autenticacao.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return registration;
    }

    static final class Observador {
        private final Path output;
        private final JsonMapper mapper;
        private final ThreadLocal<Medicao> current = new ThreadLocal<>();

        Observador(Path output, JsonMapper mapper) {
            this.output = output;
            this.mapper = mapper;
        }

        void iniciar(String requestId, String method) {
            if (current.get() != null) {
                throw new IllegalStateException("SQL01 recusa contexto de request sobreposto.");
            }
            current.set(new Medicao(requestId, method));
        }

        String inspect(String sql) {
            var measurement = current.get();
            if (measurement != null) {
                measurement.registrar(sql);
            }
            return sql;
        }

        void terminar(String route, int status, boolean completed, boolean async)
                throws IOException {
            var measurement = current.get();
            current.remove();
            if (measurement == null) return;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("demand", "QUAL-CONF01-SQL01");
            result.put("requestId", measurement.requestId);
            result.put("startedUtc", measurement.started.toString());
            result.put("finishedUtc", Instant.now().toString());
            result.put("method", measurement.method);
            result.put("routePattern", route);
            result.put("httpStatus", status);
            result.put("chainCompleted", completed);
            result.put("asyncStarted", async);
            result.put("serverRequestElapsedNanos", System.nanoTime() - measurement.nanoStart);
            result.put("statementsInspectedPrepared", measurement.count);
            result.put("maxParameterPlaceholdersObserved", measurement.maxParameters);
            result.put("parameterPlaceholdersObserved", measurement.parameters);
            result.put("springTransactionActiveStatements", measurement.transactionStatements);
            result.put("springReadOnlyStatements", measurement.readOnlyStatements);
            result.put("springIsolationLevels", measurement.isolationLevels);
            result.put("shapesOmitted", measurement.omitted);
            result.put("sanitizedSqlShapes", new ArrayList<>(measurement.shapes.values()));
            result.put(
                    "limit",
                    "Statements inspected before JDBC execution, not rows/SQLCPU/JDBC timings. "
                            + "Thread-local synchronous request only; no global delta, binds, "
                            + "literals, querystring, headers, cookies, tokens or body recorded.");
            Files.writeString(
                    output.resolve(measurement.requestId + ".json"),
                    mapper.writeValueAsString(result),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW);
        }
    }

    static final class Medicao {
        final String requestId;
        final String method;
        final Instant started = Instant.now();
        final long nanoStart = System.nanoTime();
        final Map<String, Map<String, Object>> shapes = new LinkedHashMap<>();
        final List<Integer> isolationLevels = new ArrayList<>();
        int count;
        int parameters;
        int maxParameters;
        int transactionStatements;
        int readOnlyStatements;
        int omitted;

        Medicao(String requestId, String method) {
            this.requestId = requestId;
            this.method = method;
        }

        void registrar(String sql) {
            var sanitized = sanitizar(sql);
            count++;
            parameters += sanitized.parameters();
            maxParameters = Math.max(maxParameters, sanitized.parameters());
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                transactionStatements++;
            }
            if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
                readOnlyStatements++;
            }
            Integer isolation =
                    TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
            if (isolation != null && !isolationLevels.contains(isolation))
                isolationLevels.add(isolation);
            var existing = shapes.get(sanitized.shape());
            if (existing != null) {
                existing.put("count", (Integer) existing.get("count") + 1);
            } else if (shapes.size() < 256 && sanitized.shape().length() <= 65536) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("shape", sanitized.shape());
                item.put("parameterPlaceholders", sanitized.parameters());
                item.put("count", 1);
                shapes.put(sanitized.shape(), item);
            } else {
                omitted++;
            }
        }
    }

    record SqlSanitizado(String shape, int parameters) {}

    static SqlSanitizado sanitizar(String sql) {
        StringBuilder shape = new StringBuilder();
        int parameters = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"' || c == '[') {
                char end = c == '[' ? ']' : c;
                shape.append(c == '\'' ? "<literal>" : "<identifier>");
                while (++i < sql.length()) {
                    if (sql.charAt(i) != end) continue;
                    if (i + 1 < sql.length() && sql.charAt(i + 1) == end) i++;
                    else break;
                }
            } else if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (++i < sql.length() && sql.charAt(i) != '\n') {}
                shape.append(' ');
            } else if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                int depth = 1;
                i += 2;
                while (i < sql.length() && depth > 0) {
                    if (i + 1 < sql.length() && sql.charAt(i) == '/' && sql.charAt(i + 1) == '*') {
                        depth++;
                        i += 2;
                    } else if (i + 1 < sql.length()
                            && sql.charAt(i) == '*'
                            && sql.charAt(i + 1) == '/') {
                        depth--;
                        i += 2;
                    } else i++;
                }
                i--;
                shape.append(' ');
            } else {
                if (c == '?') parameters++;
                shape.append(c);
            }
        }
        String normalized =
                shape.toString()
                        .replaceAll("(?i)(?<![A-Za-z0-9_])0x[0-9a-f]*(?![A-Za-z0-9_])", "<number>")
                        .replaceAll(
                                "(?<![A-Za-z0-9_])[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?(?![A-Za-z0-9_])",
                                "<number>")
                        .replaceAll("\\s+", " ")
                        .strip();
        return new SqlSanitizado(normalized, parameters);
    }

    static final class Filtro extends OncePerRequestFilter {
        private static final Logger LOG = LoggerFactory.getLogger(Filtro.class);
        private final Observador observer;

        Filtro(Observador observer) {
            this.observer = observer;
        }

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return !request.getRequestURI().startsWith("/api/v1/")
                    && !request.getRequestURI().equals("/api/auth/eu");
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            Object existing = request.getAttribute(IdentificacaoOperacaoFilter.ATRIBUTO);
            String id =
                    existing instanceof String value
                                    && value.matches("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
                            ? value
                            : UUID.randomUUID().toString();
            String method =
                    switch (request.getMethod()) {
                        case "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "TRACE" ->
                                request.getMethod();
                        default -> "OTHER";
                    };
            observer.iniciar(id, method);
            boolean completed = false;
            try {
                chain.doFilter(request, response);
                completed = true;
            } finally {
                Object matched =
                        request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
                String route =
                        matched instanceof String value && value.matches("/api/[A-Za-z0-9/{}/_-]+")
                                ? value
                                : "/api/{unresolved}";
                try {
                    observer.terminar(
                            route, response.getStatus(), completed, request.isAsyncStarted());
                } catch (Exception error) {
                    // O diagnostico nao substitui resultado/erro da operacao nem publica detalhes.
                    LOG.warn(
                            "SQL01 evidence refused; request={}; type={}",
                            id,
                            error.getClass().getSimpleName());
                }
            }
        }
    }
}
