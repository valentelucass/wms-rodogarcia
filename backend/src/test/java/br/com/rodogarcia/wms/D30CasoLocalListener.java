package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.core.Ordered;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

/** Liga observações HTTP da FINAL ao método atual; não cria ou executa um caso adicional. */
public final class D30CasoLocalListener implements TestExecutionListener, Ordered {
    private static final ConcurrentHashMap<String, String> CASOS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Map<String, Object>>> CAPTURAS =
            new ConcurrentHashMap<>();

    static void observacao(String classe, Map<String, Object> prova) {
        String caso = caso(classe);
        if (!caso.equals(prova.get("caso")))
            throw new IllegalStateException("Captura GET de outro caso D30.");
        CAPTURAS.get(classe).add(prova);
    }

    static String caso(String classe) {
        String caso = CASOS.get(classe);
        if (caso == null) throw new IllegalStateException("GET D30 sem método local identificado.");
        return caso;
    }

    @Override
    public int getOrder() {
        return 1900;
    }

    @Override
    public void beforeTestMethod(TestContext context) {
        if (!"D30_FINAL".equals(System.getProperty("wms.test.leituras.sem-efeito"))) return;
        String classe = context.getTestClass().getSimpleName();
        String metodo = context.getTestClass().getName() + "#" + context.getTestMethod().getName();
        if (CASOS.putIfAbsent(classe, metodo) != null)
            throw new IllegalStateException("Métodos locais concorrentes no mesmo contexto D30.");
        CAPTURAS.put(classe, new CopyOnWriteArrayList<>());
    }

    @Override
    public void afterTestMethod(TestContext context) throws Exception {
        String classe = context.getTestClass().getSimpleName();
        var capturas = CAPTURAS.remove(classe);
        CASOS.remove(classe);
        D30QueriesProviderObservador.conferir(context);
        if (capturas == null) return;
        for (var p : capturas) {
            assertThat(p.get("caso"))
                    .isEqualTo(
                            context.getTestClass().getName()
                                    + "#"
                                    + context.getTestMethod().getName());
            if (!Boolean.TRUE.equals(p.get("concorrenciaHTTP")))
                assertThat(p.get("fisicoIgual"))
                        .as("GET D30 " + p.get("rotaObservada") + " captura " + p.get("captura"))
                        .isEqualTo(Boolean.TRUE);
        }
    }
}
