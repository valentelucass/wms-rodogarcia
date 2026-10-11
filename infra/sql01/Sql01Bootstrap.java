import java.io.DataInputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** SQL01: three bounded frames on the private child stdin; never secret args/env/files. */
class Sql01Bootstrap {
    private static final String[] KEYS = {
        "wms.database.password", "wms.auth.private-key", "wms.auth.bootstrap-hash"
    };

    // Opt-in public metadata only. No messages, stack, arguments or private frames.
    static Path diagnosticPath(String[] args) throws Exception {
        if (args.length != 12) return null;
        if (!java.util.UUID.fromString(args[10]).toString().equals(args[10]))
            throw new IllegalStateException();
        Path root = Path.of(args[6]).toRealPath();
        Path expected = root.resolve("orchestracao/.runtime/qual-conf01/sql01/prumo/runs")
                .resolve(args[10]).resolve("bootstrap-diagnostic.json");
        Path requested = Path.of(args[11]).toAbsolutePath().normalize();
        if (!requested.equals(expected) || Files.exists(requested)) throw new IllegalStateException();
        for (Path parent = requested.getParent(); parent != null; parent = parent.getParent()) {
            if (Files.isSymbolicLink(parent) || !parent.toRealPath().equals(parent))
                throw new IllegalStateException();
        }
        return requested;
    }

    static String safeType(Throwable error) {
        String type = error.getClass().getName();
        return type.matches("[A-Za-z0-9_.$]{1,192}") ? type : "UNKNOWN";
    }

    static void recordDiagnostic(Path path, String runId, String phase, Throwable error) {
        if (path == null) return;
        var json = new StringBuilder("{\"id\":\"QUAL-CONF01-SQL01\",\"runId\":\"")
                .append(runId).append("\",\"pid\":").append(ProcessHandle.current().pid())
                .append(",\"phase\":\"").append(phase).append("\",\"type\":\"")
                .append(safeType(error)).append("\",\"chain\":[");
        Throwable current = error;
        for (int count = 0; current != null && count < 8; count++) {
            if (count != 0) json.append(',');
            json.append('"').append(safeType(current)).append('"');
            current = current.getCause();
        }
        json.append("],\"exitCode\":20,\"messagesIncluded\":false}");
        try {
            Files.writeString(path, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (Exception unavailable) {
            // A missing diagnostic remains a limit; never overwrite or use another path.
        }
    }

    static String frame(DataInputStream input, int maximum, boolean emptyAllowed) throws Exception {
        int size = input.readInt();
        if (size < (emptyAllowed ? 0 : 1) || size > maximum) throw new IllegalStateException();
        byte[] bytes = new byte[size];
        try {
            input.readFully(bytes);
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } finally { Arrays.fill(bytes, (byte) 0); }
    }

    public static void main(String[] args) {
        Path diagnostic = null;
        String phase = "ARGUMENTS";
        try {
            if ((args.length != 11 && args.length != 12) || System.getenv().keySet().stream().anyMatch(name ->
                    name.startsWith("WMS_") || name.startsWith("SPRING_") || name.startsWith("SQL_")
                    || name.equals("JAVA_TOOL_OPTIONS") || name.equals("JDK_JAVA_OPTIONS")
                    || name.equals("_JAVA_OPTIONS"))) throw new IllegalStateException();
            diagnostic = diagnosticPath(args);
            phase = "PUBLIC_PATHS";
            Path jar = Path.of(args[0]).toRealPath();
            Path config = Path.of(args[1]).toRealPath();
            phase = "ARG_VALIDATION";
            int port = Integer.parseInt(args[2]);
            if (port < 1024 || port > 65535 || port == 1433 || port == 25580 || port == 25581
                    || !args[3].matches("[A-Za-z0-9._-]{1,128}")
                    || !args[4].matches("[A-Za-z0-9._-]{1,253}")
                    || !args[8].matches("[a-f0-9]{64}") || !args[9].matches("[a-fA-F0-9]{64}")
                    || !java.util.UUID.fromString(args[10]).toString().equals(args[10])) throw new IllegalStateException();
            phase = "JAR_HASH";
            try (var source = Files.newInputStream(jar)) {
                var digest = MessageDigest.getInstance("SHA-256");
                byte[] block = new byte[8192];
                for (int size; (size = source.read(block)) != -1;) digest.update(block, 0, size);
                if (!HexFormat.of().formatHex(digest.digest()).equalsIgnoreCase(args[9]))
                    throw new IllegalStateException();
            }
            phase = "PRIVATE_FRAMES";
            var input = new DataInputStream(System.in);
            if (input.readInt() != 0x574d5301) throw new IllegalStateException();
            String password = frame(input, 4096, false);
            String key = frame(input, 16384, false);
            String bootstrap = frame(input, 0, true);
            if (input.read() != -1 || password.length() < 32
                    || !key.matches("[A-Za-z0-9+/=]+")
                    || !bootstrap.isEmpty()) throw new IllegalStateException();
            phase = "SECRET_INSTALL";
            System.setProperty(KEYS[0], password);
            System.setProperty(KEYS[1], key);
            // Require an existing principal: empty hash makes absence stop before bootstrap writes.
            System.setProperty(KEYS[2], "");
            password = null; key = null; bootstrap = null;
            System.setProperty("wms.database.tls.trust-store-password", "projetos-public-cert");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                for (String name : KEYS) System.clearProperty(name);
                System.clearProperty("wms.database.tls.trust-store-password");
            }));
            phase = "APPLICATION_ARGUMENTS";
            String[] application = {
                "--spring.profiles.active=sqlserver-dev",
                "--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-dev.properties",
                "--spring.config.additional-location=" + config.toUri(),
                "--server.address=127.0.0.1", "--server.port=" + port,
                "--wms.cadastros.enabled=true", "--spring.autoconfigure.exclude=",
                "--spring.jpa.hibernate.ddl-auto=validate", "--spring.jpa.generate-ddl=false",
                "--spring.sql.init.mode=never", "--spring.flyway.enabled=false", "--spring.liquibase.enabled=false",
                "--wms.database.host=127.0.0.1", "--wms.database.port=1433",
                "--wms.database.name=WMS_DEV", "--wms.database.user=WMSDEV",
                "--wms.database.confirmed-target=127.0.0.1:1433/WMS_DEV",
                "--wms.database.confirmed-server=" + args[3],
                "--wms.database.tls.certificate-host=" + args[4],
                "--wms.database.tls.trust-store=" + args[5],
                "--wms.identity.issuer=https://wms.localhost",
                "--wms.identity.jwk-set-uri=https://wms.localhost/api/auth/jwks",
                "--wms.identity.audience=wms-dev", "--wms.auth.enabled=true",
                "--wms.auth.origin=http://127.0.0.1:" + port,
                "--wms.auth.proxy-origin=", "--wms.auth.secure-cookie=false",
                "--wms.auth.bootstrap-hash=",
                "--wms.sql01.observation.enabled=true", "--wms.sql01.repository-root=" + args[6],
                "--wms.sql01.observation.dir=" + args[7], "--wms.sql01.candidate-source-hash=" + args[8],
                "--wms.sql01.candidate-jar-sha256=" + args[9], "--wms.sql01.run-id=" + args[10]
            };
            phase = "JAR_CLASS_LOADING";
            var loader = new URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, ClassLoader.getSystemClassLoader());
            var entry = Class.forName("org.springframework.boot.loader.launch.JarLauncher", true, loader);
            phase = "JAR_MAIN_LOOKUP";
            var main = entry.getMethod("main", String[].class);
            phase = "APPLICATION_LAUNCH";
            main.invoke(null, (Object) application);
        } catch (Throwable refused) {
            for (String name : KEYS) System.clearProperty(name);
            System.clearProperty("wms.database.tls.trust-store-password");
            recordDiagnostic(diagnostic, diagnostic == null ? "" : args[10], phase, refused);
            System.err.println("SQL01_BOOTSTRAP_REFUSED");
            System.exit(20);
        }
    }
}
