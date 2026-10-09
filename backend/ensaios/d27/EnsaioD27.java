import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Auxiliar D27: sem decoder alternativo no app e sem escrita SQL de negócio. */
public class EnsaioD27 {
    final JsonMapper json = JsonMapper.builder().build();
    final Path backend = Path.of("").toAbsolutePath(), evidence = backend.resolve("evidencias");
    final String round =
            "D27"
                    + UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 8)
                            .toUpperCase(Locale.ROOT);
    final List<Map<String, Object>> cases = Collections.synchronizedList(new ArrayList<>());
    final Map<String, Object> ids = new LinkedHashMap<>();
    final HttpClient http =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();
    FixtureHttps fixture;
    Process app;
    String base;
    String phase = "preparo";
    String blocker;
    long clientId, warehouseId;
    JsonNode product, pack;
    final String secret;

    EnsaioD27() {
        this(System.getenv().getOrDefault("WMS_DB_PASSWORD", ""));
    }

    EnsaioD27(String testSecret) {
        secret = testSecret;
    }

    static Map<String, Object> m(Object... pairs) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < pairs.length; i += 2) map.put((String) pairs[i], pairs[i + 1]);
        return map;
    }

    Map<String, Object> command() {
        return m(
                "operacaoId",
                UUID.randomUUID().toString(),
                "motivo",
                round + " ensaio ficticio rastreavel");
    }

    Map<String, Object> version(JsonNode p) {
        var c = command();
        c.put("versao", p.get("versao").longValue());
        return c;
    }

    void save() throws Exception {
        var doc =
                m(
                        "rodada",
                        round,
                        "fase",
                        phase,
                        "bloqueio",
                        blocker,
                        "alvo",
                        "127.0.0.1:1433/WMS_DEV",
                        "login",
                        "WMSDEV",
                        "escritaNegocio",
                        "somente HTTP",
                        "cases",
                        cases,
                        "ids",
                        ids,
                        "segredoRegistrado",
                        false);
        Files.writeString(
                evidence.resolve("d27-" + round + "-http.json"),
                json.writerWithDefaultPrettyPrinter().writeValueAsString(doc));
        Files.writeString(
                evidence.resolve("d27-persistencia-pedido.json"),
                json.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(
                                m(
                                        "rodada",
                                        round,
                                        "fase",
                                        phase,
                                        "ids",
                                        ids,
                                        "consulta",
                                        "SELECT somente WMS_DEV; preservar fixtures IT/HTTP;"
                                                + " confrontar GETs nos casos",
                                        "arquivoHTTP",
                                        "d27-" + round + "-http.json")));
    }

    String token(String role, Map<String, Object> overrides) throws Exception {
        var now = Instant.now();
        var b =
                new JWTClaimsSet.Builder()
                        .issuer(fixture.issuer)
                        .audience("wms-d27-local")
                        .subject(round + "-" + role)
                        .issueTime(java.util.Date.from(now))
                        .expirationTime(java.util.Date.from(now.plusSeconds(600)))
                        .claim("wms_perfil", role)
                        .claim(
                                "wms_clientes",
                                clientId == 0 ? List.of() : List.of(Long.toString(clientId)))
                        .claim(
                                "wms_armazens",
                                warehouseId == 0 ? List.of() : List.of(Long.toString(warehouseId)));
        overrides.forEach(b::claim);
        var jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256)
                                .keyID(fixture.jwt.getKeyID())
                                .build(),
                        b.build());
        jwt.sign(new RSASSASigner(fixture.jwt));
        return jwt.serialize();
    }

    JsonNode call(
            String name, String method, String route, Object payload, String role, int expected)
            throws Exception {
        return callToken(
                name,
                method,
                route,
                payload,
                role,
                role == null ? null : token(role, Map.of()),
                expected);
    }

    JsonNode callToken(
            String name,
            String method,
            String route,
            Object payload,
            String role,
            String jwt,
            int expected)
            throws Exception {
        var req =
                HttpRequest.newBuilder(URI.create(base + route))
                        .timeout(Duration.ofSeconds(40))
                        .header("X-Request-Id", round + "-" + UUID.randomUUID());
        if (jwt != null) req.header("Authorization", "Bearer " + jwt);
        if (payload != null) req.header("Content-Type", "application/json");
        req.method(
                method,
                payload == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)));
        var response = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        return recordResponse(name, method, route, payload, role, expected, response, false);
    }

    JsonNode recordResponse(
            String name,
            String method,
            String route,
            Object payload,
            String role,
            int expected,
            HttpResponse<String> response,
            boolean concurrent)
            throws Exception {
        String body = response.body();
        boolean leaked =
                (!secret.isEmpty() && body.contains(secret))
                        || java.util.regex.Pattern.compile(
                                        "(?i)(\\\"(?:password|senha|authorization|access_token|refresh_token)\\\"\\s*:|bearer\\s+[a-z0-9._-]+|eyJ[a-z0-9_-]+\\.eyJ[a-z0-9_-]+\\.[a-z0-9_-]+)")
                                .matcher(body)
                                .find();
        boolean cookie = response.headers().firstValue("Set-Cookie").isPresent();
        boolean requestId =
                response.headers().firstValue("X-Request-Id").filter(v -> !v.isBlank()).isPresent();
        boolean pass =
                (concurrent
                                ? (response.statusCode() == 200 || response.statusCode() == 409)
                                : response.statusCode() == expected)
                        && !leaked
                        && !cookie
                        && requestId;
        JsonNode parsed = body.isBlank() ? json.readTree("{}") : json.readTree(body);
        cases.add(
                m(
                        "caso",
                        name,
                        "metodo",
                        method,
                        "rota",
                        route,
                        "perfil",
                        role == null ? "anonimo" : role,
                        "expected",
                        concurrent ? "um 200 outro 409" : expected,
                        "actual",
                        response.statusCode(),
                        "estado",
                        pass ? (concurrent ? "confrontar conjunto" : "aprovado") : "falhou",
                        "cookiePresente",
                        cookie,
                        "segredoPresente",
                        leaked,
                        "requestIdPresente",
                        requestId,
                        "payload",
                        payload,
                        "resposta",
                        leaked ? m("omitida", true) : parsed));
        save();
        if (!pass)
            throw new CaseFailure(
                    name
                            + " expected="
                            + expected
                            + " actual="
                            + response.statusCode()
                            + " cookie="
                            + cookie
                            + " leak="
                            + leaked);
        return parsed;
    }

    static String processOutputLimited(Process process, long milliseconds) throws Exception {
        var readers = Executors.newVirtualThreadPerTaskExecutor();
        var out = readers.submit(() -> process.getInputStream().readAllBytes());
        var err = readers.submit(() -> process.getErrorStream().readAllBytes());
        try {
            if (!process.waitFor(milliseconds, TimeUnit.MILLISECONDS))
                throw new IllegalStateException("D27_JCMD_PROPRIO_TIMEOUT");
            return new String(
                            out.get(1000, TimeUnit.MILLISECONDS),
                            java.nio.charset.StandardCharsets.UTF_8)
                    + new String(
                            err.get(1000, TimeUnit.MILLISECONDS),
                            java.nio.charset.StandardCharsets.UTF_8);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(2000, TimeUnit.MILLISECONDS);
            }
            process.getInputStream().close();
            process.getErrorStream().close();
            out.cancel(true);
            err.cancel(true);
            readers.shutdownNow();
        }
    }

    void assertion(String name, boolean pass, Object expected, Object actual) throws Exception {
        cases.add(
                m(
                        "caso",
                        name,
                        "tipo",
                        "GET/estado",
                        "expected",
                        expected,
                        "actual",
                        actual,
                        "estado",
                        pass ? "aprovado" : "falhou"));
        save();
        if (!pass) throw new CaseFailure(name);
    }

    static class CaseFailure extends Exception {
        CaseFailure(String s) {
            super(s);
        }
    }

    SQLServerDataSource sqlDataSource() {
        var ds = new SQLServerDataSource();
        ds.setServerName("127.0.0.1");
        ds.setPortNumber(1433);
        ds.setDatabaseName("WMS_DEV");
        ds.setUser("WMSDEV");
        ds.setPassword(secret);
        ds.setEncrypt("true");
        ds.setTrustServerCertificate(false);
        ds.setTrustStore(System.getenv("WMS_DB_TRUST_STORE"));
        ds.setTrustStorePassword(System.getenv("WMS_DB_TRUST_STORE_PASSWORD"));
        ds.setHostNameInCertificate(System.getenv("WMS_DB_CERTIFICATE_HOST"));
        ds.setLoginTimeout(10);
        return ds;
    }

    void precheck(String stage, boolean empty) throws Exception {
        phase = stage;
        if (!"WMSDEV".equals(System.getenv("WMS_DB_USER"))
                || !"127.0.0.1".equals(System.getenv("WMS_DB_HOST"))
                || !"WMS_DEV".equals(System.getenv("WMS_DB_NAME"))
                || !"1433".equals(System.getenv("WMS_DB_PORT"))
                || secret.length() < 32)
            throw new IllegalStateException("D27_ALVO_CREDENCIAL_RECUSADOS");
        var ds = sqlDataSource();
        try (var c = ds.getConnection();
                var st = c.createStatement()) {
            st.execute(
                    "SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT"
                            + " ON; SET CONCAT_NULL_YIELDS_NULL ON; SET QUOTED_IDENTIFIER ON; SET"
                            + " NUMERIC_ROUNDABORT OFF;");
            var row = new LinkedHashMap<String, Object>();
            try (var r =
                    st.executeQuery(
                            "SELECT DB_NAME()"
                                + " banco,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName'))"
                                + " servidor,ORIGINAL_LOGIN() login,USER_NAME()"
                                + " usuario,IS_SRVROLEMEMBER('sysadmin')"
                                + " admin,IS_MEMBER('db_owner') ownerRole,IS_MEMBER('db_ddladmin')"
                                + " ddlRole,HAS_DBACCESS(N'WMS_PROD')"
                                + " prod,HAS_PERMS_BY_NAME(DB_NAME(),'DATABASE','CREATE SCHEMA')"
                                + " createSchema,HAS_PERMS_BY_NAME('wms','SCHEMA','ALTER')"
                                + " alterSchema,CONNECTIONPROPERTY('local_net_address')"
                                + " endereco,CONNECTIONPROPERTY('local_tcp_port') porta")) {
                if (!r.next()) throw new IllegalStateException("D27_ALVO_AUSENTE");
                for (int i = 1; i <= r.getMetaData().getColumnCount(); i++)
                    row.put(r.getMetaData().getColumnLabel(i), r.getObject(i));
                row.put(
                        "tipoPortaJDBC",
                        r.getObject("porta") == null
                                ? "NULL"
                                : r.getObject("porta").getClass().getSimpleName());
                row.put("porta", r.getInt("porta"));
            }
            Files.writeString(
                    evidence.resolve("d27-" + round + "-alvo-observado-" + stage + ".json"),
                    json.writerWithDefaultPrettyPrinter().writeValueAsString(row));
            if (!"WMS_DEV".equals(row.get("banco"))
                    || !System.getenv("WMS_DB_CONFIRMED_SERVER").equals(row.get("servidor"))
                    || !"WMSDEV".equals(row.get("login"))
                    || !"WMSDEV".equals(row.get("usuario"))
                    || !"127.0.0.1".equals(row.get("endereco"))
                    || !Integer.valueOf(1433).equals(row.get("porta")))
                throw new IllegalStateException("D27_IDENTIDADE_ALVO_REAL_RECUSADOS");
            row.put("tlsClienteEncrypt", true);
            row.put("trustServerCertificate", false);
            row.put(
                    "observacaoTLS",
                    "Conexao JDBC obrigatoria com validacao truststore/nome; DMV restrita nao"
                            + " consultada pela app. Atestacao servidor separada Prumo.");
            for (var name :
                    List.of("admin", "ownerRole", "ddlRole", "prod", "createSchema", "alterSchema"))
                if (!Integer.valueOf(0).equals(row.get(name)))
                    throw new IllegalStateException("D27_PRIVILEGIO_RECUSADO_" + name);
            var expectedHistory = new LinkedHashMap<String, Integer>();
            int[] baseline = {
                -505625255,
                815010621,
                1941824532,
                30048122,
                -58574332,
                586191463,
                1926473098,
                -585549635,
                1648171073
            };
            for (int i = 0; i < baseline.length; i++)
                expectedHistory.put(Integer.toString(i + 1), baseline[i]);
            String checksum10 = System.getenv("WMS_D27_V10_CHECKSUM");
            if (checksum10 != null) expectedHistory.put("10", Integer.valueOf(checksum10));
            var history = new LinkedHashMap<String, Integer>();
            try (var r =
                    st.executeQuery(
                            "SELECT version,checksum,success FROM wms.flyway_schema_history WHERE"
                                    + " type='SQL' ORDER BY installed_rank")) {
                while (r.next()) {
                    if (!r.getBoolean(3) || history.put(r.getString(1), r.getInt(2)) != null)
                        throw new IllegalStateException("D27_HISTORICO_RECUSADO");
                }
            }
            if (!history.equals(expectedHistory))
                throw new IllegalStateException("D27_HISTORICO_RECUSADO");
            row.put("historicoSQL", history);
            var counts = new LinkedHashMap<String, Long>();
            var names = new ArrayList<String>();
            try (var r =
                    st.executeQuery(
                            "SELECT name FROM sys.tables WHERE schema_id=SCHEMA_ID('wms') AND"
                                    + " name<>'flyway_schema_history' ORDER BY name")) {
                while (r.next()) names.add(r.getString(1));
            }
            if (names.size() != 64) throw new IllegalStateException("D27_CATALOGO_RECUSADO");
            for (var name : names) {
                try (var r =
                        st.executeQuery(
                                "SELECT COUNT_BIG(*) FROM wms.[" + name.replace("]", "]]") + "]")) {
                    r.next();
                    counts.put(name, r.getLong(1));
                    if (empty && r.getLong(1) != 0)
                        throw new IllegalStateException("D27_BANCO_NAO_VAZIO");
                }
            }
            row.put("contagens", counts);
            row.put("driver", c.getMetaData().getDriverVersion());
            Files.writeString(
                    evidence.resolve("d27-" + round + "-precheck-" + stage + ".json"),
                    json.writerWithDefaultPrettyPrinter().writeValueAsString(row));
        }
    }

    void startJar(Path jar, Path work) throws Exception {
        try (var in = Files.newInputStream(jar)) {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] chunk = new byte[65536];
            int n;
            while ((n = in.read(chunk)) != -1) digest.update(chunk, 0, n);
            ids.put("jarSha256", HexFormat.of().withUpperCase().formatHex(digest.digest()));
        }
        ids.put("jarArquivo", backend.relativize(jar.toAbsolutePath()).toString());
        ids.put("javaRuntime", System.getProperty("java.version"));
        fixture = new FixtureHttps(work);
        int port;
        try (var socket = new java.net.ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        base = "http://127.0.0.1:" + port;
        var pb =
                new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                        "-Xmx768m",
                        "-Djavax.net.ssl.trustStore=" + fixture.trustStore,
                        "-Djavax.net.ssl.trustStorePassword=d27-public-cert",
                        "-Djavax.net.ssl.trustStoreType=PKCS12",
                        "-jar",
                        jar.toString());
        restrictEnv(pb);
        pb.environment().put("SPRING_PROFILES_ACTIVE", "sqlserver-dev");
        pb.environment().put("WMS_PORT", Integer.toString(port));
        pb.environment().put("WMS_OIDC_ISSUER", fixture.issuer);
        pb.environment().put("WMS_OIDC_JWK_SET_URI", fixture.issuer + "/jwks");
        pb.environment().put("WMS_OIDC_AUDIENCE", "wms-d27-local");
        app = pb.start();
        ids.put("jarPid", app.pid());
        ids.put("porta", port);
        ids.put("issuerPorta", fixture.server.getAddress().getPort());
        ids.put("jarEncerrado", false);
        var capture = capture(app, "jar");
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        boolean ready = false;
        while (System.nanoTime() < deadline && app.isAlive()) {
            try {
                var r =
                        http.send(
                                HttpRequest.newBuilder(URI.create(base + "/api/v1/status"))
                                        .timeout(Duration.ofSeconds(2))
                                        .build(),
                                HttpResponse.BodyHandlers.discarding());
                if (r.statusCode() == 200) {
                    ready = true;
                    break;
                }
            } catch (Exception ignored) {
            }
            Thread.sleep(500);
        }
        if (!ready) {
            stopApp();
            capture.join(10000);
            throw new IllegalStateException("D27_JAR_INICIALIZACAO_FALHOU_VER_LOG_SANITIZADO");
        }
    }

    void restrictEnv(ProcessBuilder pb) {
        var inherited = new HashMap<>(pb.environment());
        pb.environment().clear();
        for (var key :
                List.of(
                        "SystemRoot",
                        "WINDIR",
                        "COMSPEC",
                        "TEMP",
                        "TMP",
                        "LOCALAPPDATA",
                        "APPDATA",
                        "USERPROFILE",
                        "PATH",
                        "PATHEXT",
                        "JAVA_HOME",
                        "MAVEN_USER_HOME"))
            if (inherited.containsKey(key)) pb.environment().put(key, inherited.get(key));
        for (var key :
                List.of(
                        "WMS_DB_HOST",
                        "WMS_DB_PORT",
                        "WMS_DB_NAME",
                        "WMS_DB_USER",
                        "WMS_DB_PASSWORD",
                        "WMS_DB_CONFIRMED_TARGET",
                        "WMS_DB_CONFIRMED_SERVER",
                        "WMS_DB_CERTIFICATE_HOST",
                        "WMS_DB_TRUST_STORE",
                        "WMS_DB_TRUST_STORE_PASSWORD"))
            pb.environment().put(key, System.getenv(key));
    }

    Thread capture(Process p, String kind) {
        var t =
                Thread.ofPlatform()
                        .start(
                                () -> {
                                    try {
                                        var out = new java.io.ByteArrayOutputStream();
                                        var err =
                                                Thread.ofPlatform()
                                                        .start(
                                                                () -> {
                                                                    try {
                                                                        p.getErrorStream()
                                                                                .transferTo(out);
                                                                    } catch (Exception ignored) {
                                                                    }
                                                                });
                                        p.getInputStream().transferTo(out);
                                        err.join();
                                        String log =
                                                out.toString(
                                                        java.nio.charset.StandardCharsets.UTF_8);
                                        if (!secret.isEmpty())
                                            log = log.replace(secret, "[SEGREDO_OMITIDO]");
                                        Files.writeString(
                                                evidence.resolve(
                                                        "d27-" + round + "-" + kind + ".log"),
                                                log);
                                    } catch (Exception e) {
                                        blocker = "D27_CAPTURA_FALHOU";
                                    }
                                });
        return t;
    }

    void stopApp() throws Exception {
        if (app != null && app.isAlive()) {
            app.destroy();
            if (!app.waitFor(15, TimeUnit.SECONDS)) {
                app.destroyForcibly();
                app.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        var x = new EnsaioD27();
        try {
            x.ids.put("etapa", args[0]);
            if (args.length > 2) {
                if (!args[2].matches("D27[A-F0-9]{8}"))
                    throw new IllegalStateException("D27_BASE_INVALIDA");
                var prior =
                        x.json.readTree(
                                Files.readString(
                                        x.evidence.resolve("d27-" + args[2] + "-http.json")));
                x.ids.putAll(
                        x.json.convertValue(
                                prior.get("ids"),
                                new tools.jackson.core.type.TypeReference<
                                        Map<String, Object>>() {}));
                x.ids.put("retomadaDe", args[2]);
                x.clientId = ((Number) x.ids.get("clienteId")).longValue();
                x.warehouseId = ((Number) x.ids.get("armazemId")).longValue();
            }
            x.ids.put("etapa", args[0]);
            var work = x.backend.resolve("target-d27-helper/" + x.round);
            Files.createDirectories(work);
            x.precheck("inicio", false);
            x.startJar(Path.of(args[1]), work);
            new PendenciasD27(x).run(args[0]);
            x.precheck("fim", false);
            x.phase = "concluido";
        } catch (Exception e) {
            x.blocker =
                    e instanceof CaseFailure
                            ? e.getMessage()
                            : e instanceof IllegalStateException
                                            && e.getMessage() != null
                                            && e.getMessage().startsWith("D27_")
                                    ? e.getMessage()
                                    : "D27_FALHA_VER_EVIDENCIA";
            x.phase = "falhou";
        } finally {
            x.stopApp();
            if (x.fixture != null) x.fixture.close();
            x.ids.put("jarEncerrado", x.app == null || !x.app.isAlive());
            x.ids.put("issuerEncerrado", true);
            x.ids.put("portasLivres", x.portFree("porta") && x.portFree("issuerPorta"));
            x.save();
        }
        System.out.println(
                "D27 rodada="
                        + x.round
                        + " fase="
                        + x.phase
                        + " bloqueio="
                        + (x.blocker == null ? "nenhum" : "EVIDENCIA_PRESERVADA"));
        if (x.blocker != null) System.exit(1);
    }

    boolean portFree(String key) {
        if (!ids.containsKey(key)) return true;
        try (var socket = new java.net.ServerSocket()) {
            socket.setReuseAddress(true);
            socket.bind(
                    new java.net.InetSocketAddress(
                            "127.0.0.1", ((Number) ids.get(key)).intValue()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
