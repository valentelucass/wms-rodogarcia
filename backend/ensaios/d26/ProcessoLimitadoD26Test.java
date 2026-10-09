import java.time.Duration;
import java.util.concurrent.TimeUnit;

/** Processo fictício próprio sem SQL, para verificar prazo anterior à leitura completa. */
public class ProcessoLimitadoD26Test {
    public static void main(String[] args) throws Exception {
        var javaExe = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java.exe");
        var process =
                new ProcessBuilder(
                                javaExe.toString(),
                                "-cp",
                                System.getProperty("java.class.path"),
                                "EnsaioD26",
                                "fixture-processo-bloqueado")
                        .start();
        long begin = System.nanoTime();
        boolean refused = false;
        try {
            EnsaioD26.processOutputLimited(process, 400);
        } catch (IllegalStateException e) {
            refused = e.getMessage().equals("D26_JCMD_PROPRIO_TIMEOUT");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(2, TimeUnit.SECONDS);
            }
        }
        long elapsed = Duration.ofNanos(System.nanoTime() - begin).toMillis();
        boolean pass = refused && !process.isAlive() && elapsed < 4000;
        System.out.println(
                "{\"caso\":\"timeout-real-processo-proprio\",\"semSQL\":true,\"recusou\":"
                        + refused
                        + ",\"processoEncerrado\":"
                        + !process.isAlive()
                        + ",\"elapsedMs\":"
                        + elapsed
                        + ",\"aprovado\":"
                        + pass
                        + "}");
        if (!pass) throw new IllegalStateException("D26_TIMEOUT_REGRESSAO_FALHOU");
    }
}
