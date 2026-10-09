package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.json.JsonMapper;

/** Observador test-only da FINAL. Não serializa headers, tokens, corpos ou credenciais. */
public final class D30GetSemEfeitoFilter extends OncePerRequestFilter implements Ordered {
    private final String classe;
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper;
    private final AtomicInteger escritores = new AtomicInteger();
    private final AtomicLong epoca = new AtomicLong();

    public D30GetSemEfeitoFilter(String classe, JdbcTemplate jdbc, JsonMapper mapper) {
        this.classe = classe;
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            chain.doFilter(request, response);
            return;
        }
        if (!"GET".equals(request.getMethod())) {
            escritores.incrementAndGet();
            epoca.incrementAndGet();
            try {
                chain.doFilter(request, response);
            } finally {
                epoca.incrementAndGet();
                escritores.decrementAndGet();
            }
            return;
        }
        String caso = D30CasoLocalListener.caso(classe);
        long inicio = epoca.get();
        boolean concorrente = escritores.get() != 0;
        Map<String, List<Map<String, Object>>> antes =
                concorrente ? null : D30FotografiaFisica.capturar(jdbc);
        var retida = new org.springframework.web.util.ContentCachingResponseWrapper(response);
        chain.doFilter(request, retida);
        concorrente |= escritores.get() != 0 || inicio != epoca.get();
        Map<String, List<Map<String, Object>>> depois =
                concorrente ? null : D30FotografiaFisica.capturar(jdbc);
        concorrente |= escritores.get() != 0 || inicio != epoca.get();
        var proof = new LinkedHashMap<String, Object>();
        proof.put("contexto", classe);
        proof.put("caso", caso);
        proof.put("metodoHTTP", "GET");
        proof.put("rotaObservada", request.getRequestURI());
        proof.put(
                "rotaContrato",
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        proof.put(
                "handler",
                handler instanceof HandlerMethod m
                        ? m.getBeanType().getName() + "#" + m.getMethod().getName()
                        : null);
        proof.put("HTTP", retida.getStatus());
        proof.put("concorrenciaHTTP", concorrente);
        proof.put(
                "oraculo",
                "GET conserva fotografia completa64tabelas WMS antes/depois; temporais tipados sem perda; sem aceitar atributos por ausência de efeitos.");
        proof.put("SQLServer", false);
        boolean iguais = !concorrente && antes.equals(depois);
        proof.put("fisicoIgual", concorrente ? null : iguais);
        proof.put(
                "estado",
                concorrente
                        ? "CONCORRENTE_SEM_ACEITE_DE_ZERO_EFEITO"
                        : "OBSERVACAO_LOCAL_COMPARADA");
        if (!concorrente) {
            proof.put("antes", fotografia(antes));
            proof.put("depois", fotografia(depois));
        }
        Path folder = pasta();
        String captura = "d30-cedro-get-observacao-" + UUID.randomUUID() + ".json";
        proof.put("captura", captura);
        Files.writeString(
                folder.resolve(captura),
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proof),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
        D30CasoLocalListener.observacao(classe, proof);
        if (!concorrente) assertThat(iguais).as("GET local conserva as64tabelas físicas").isTrue();
        retida.copyBodyToResponse();
    }

    private Map<String, Object> fotografia(Object dados) throws IOException {
        byte[] bytes = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(dados);
        String hash;
        try {
            hash =
                    HexFormat.of()
                            .withUpperCase()
                            .formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na captura D30.", e);
        }
        Path file = pasta().resolve("d30-cedro-get-foto-" + hash + ".json");
        try {
            Files.write(file, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException e) {
            if (!java.util.Arrays.equals(bytes, Files.readAllBytes(file)))
                throw new IllegalStateException("Fotografia D30 preexistente diverge do hash.");
        }
        return Map.of(
                "arquivo",
                file.getFileName().toString(),
                "sha256",
                hash,
                "bytes",
                bytes.length,
                "tabelas",
                64);
    }

    private static Path pasta() throws IOException {
        String dir = System.getProperty("wms.test.evidencias.dir");
        if (dir == null) throw new IllegalStateException("GET D30 sem destino próprio.");
        Path folder = Path.of(dir).toAbsolutePath().normalize();
        if (!folder.toString().contains("d30-cedro-"))
            throw new IllegalStateException("GET D30 fora de evidência própria.");
        Files.createDirectories(folder);
        return folder;
    }
}
