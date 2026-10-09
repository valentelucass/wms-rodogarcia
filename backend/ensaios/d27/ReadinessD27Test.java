import tools.jackson.databind.json.JsonMapper;

import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Diagnóstico offline: arquivo público somente; não carrega credenciais nem conecta SQL. */
public class ReadinessD27Test {
    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("fixtures")) {
            var x = new EnsaioD27("");
            var helper = new PendenciasD27(x);
            var file = Path.of("target-d27-helper/readiness-fixture.json");
            var checks = new ArrayList<Map<String, Object>>();
            var good =
                    EnsaioD27.m(
                            "rodada",
                            x.round,
                            "pronto",
                            true,
                            "observadorAtestado",
                            true,
                            "banco",
                            "WMS_DEV",
                            "servidor",
                            "ROD-SRVW-001",
                            "expiraUtc",
                            Instant.now().plusSeconds(60).toString());
            byte[] plain = x.json.writeValueAsBytes(good);
            Files.write(file, plain);
            check(checks, "readiness UTF8 sem BOM aceita", helper.ready(file) != null);
            byte[] bom = new byte[plain.length + 3];
            bom[0] = (byte) 239;
            bom[1] = (byte) 187;
            bom[2] = (byte) 191;
            System.arraycopy(plain, 0, bom, 3, plain.length);
            Files.write(file, bom);
            boolean redString = false;
            try {
                x.json.readTree(Files.readString(file));
            } catch (tools.jackson.core.exc.StreamReadException expected) {
                redString = true;
            }
            check(checks, "reader String anterior recusa BOM reproduzido", redString);
            check(
                    checks,
                    "readiness UTF8 BOM aceita pelo helper corrigido",
                    helper.ready(file) != null);
            for (var variant :
                    List.of(
                            EnsaioD27.m("rodada", "D2700000000"),
                            EnsaioD27.m("expiraUtc", Instant.now().minusSeconds(1).toString()),
                            EnsaioD27.m("pronto", false),
                            EnsaioD27.m("observadorAtestado", false),
                            EnsaioD27.m("banco", "WMS_PROD"),
                            EnsaioD27.m("servidor", "outro"))) {
                var copy = new LinkedHashMap<>(good);
                copy.putAll(variant);
                Files.write(file, x.json.writeValueAsBytes(copy));
                check(checks, "recusa variante " + variant.keySet(), helper.ready(file) == null);
            }
            Files.writeString(file, "{parcial");
            check(checks, "recusa gravacao parcial", helper.ready(file) == null);
            System.out.println(
                    x.json.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(
                                    EnsaioD27.m(
                                            "utc",
                                            Instant.now().toString(),
                                            "checks",
                                            checks.size(),
                                            "falhas",
                                            checks.stream()
                                                    .filter(
                                                            c ->
                                                                    Boolean.FALSE.equals(
                                                                            c.get("passou")))
                                                    .count(),
                                            "casos",
                                            checks,
                                            "sqlExecutado",
                                            false,
                                            "credencialCarregada",
                                            false)));
            return;
        }
        var bytes = Files.readAllBytes(Path.of(args[0]));
        var json = JsonMapper.builder().build();
        var result = new LinkedHashMap<String, Object>();
        result.put("arquivoPublico", args[0]);
        result.put("utc", Instant.now().toString());
        result.put("quantidadeBytes", bytes.length);
        result.put(
                "bom",
                bytes.length >= 3
                        && (bytes[0] & 255) == 239
                        && (bytes[1] & 255) == 187
                        && (bytes[2] & 255) == 191);
        for (String modo : List.of("string", "bytes")) {
            var r = new LinkedHashMap<String, Object>();
            try {
                var node =
                        modo.equals("string")
                                ? json.readTree(Files.readString(Path.of(args[0])))
                                : json.readTree(bytes);
                r.put("parse", true);
                r.put("rodadaIgual", node.path("rodada").asString().equals(args[1]));
                r.put("pronto", node.path("pronto").asBoolean(false));
                r.put("observadorAtestado", node.path("observadorAtestado").asBoolean(false));
                r.put("bancoIgual", node.path("banco").asString().equals("WMS_DEV"));
                r.put("servidorIgual", node.path("servidor").asString().equals("ROD-SRVW-001"));
                r.put("utcPublicacao", node.path("utc").asString());
                r.put("expiraUtc", node.path("expiraUtc").asString());
                r.put("sqlExecutado", false);
            } catch (Exception e) {
                r.put("parse", false);
                r.put("classeErro", e.getClass().getSimpleName());
            }
            result.put(modo, r);
        }
        System.out.println(json.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    }

    static void check(List<Map<String, Object>> checks, String name, boolean pass) {
        checks.add(EnsaioD27.m("caso", name, "passou", pass));
        if (!pass) throw new AssertionError(name);
    }
}
