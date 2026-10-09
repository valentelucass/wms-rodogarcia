package br.com.rodogarcia.wms.config;

import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/** Limite local e limitado em memória, sem confiar em X-Forwarded-For recebido do cliente. */
public class LoginProtecaoFilter extends OncePerRequestFilter {
    private final String origin;
    private final String proxyOrigin;
    private final ProblemasApi problemas;
    private final Map<String, Janela> janelas = new HashMap<>();

    private record Janela(long inicio, int chamadas) {}

    public LoginProtecaoFilter(String origin, ProblemasApi problemas) {
        this(origin, "", problemas);
    }

    public LoginProtecaoFilter(String origin, String proxyOrigin, ProblemasApi problemas) {
        this.origin = origin;
        this.proxyOrigin = proxyOrigin;
        this.problemas = problemas;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        res.setHeader("Cache-Control", "no-store");
        res.setHeader("Pragma", "no-cache");
        if (!"GET".equals(req.getMethod()) && !"HEAD".equals(req.getMethod())) {
            String receivedOrigin = req.getHeader("Origin");
            boolean trustedProxy =
                    !proxyOrigin.isEmpty()
                            && (proxyOrigin.equals(receivedOrigin)
                                    || (origin.isEmpty()
                                            && mesmaOrigemDoTunnel(req, receivedOrigin)))
                            && ("127.0.0.1".equals(req.getRemoteAddr())
                                    || "::1".equals(req.getRemoteAddr())
                                    || "0:0:0:0:0:0:0:1".equals(req.getRemoteAddr()));
            if ((origin.isEmpty() || !origin.equals(receivedOrigin)) && !trustedProxy) {
                problemas.escrever(
                        req,
                        res,
                        HttpStatus.FORBIDDEN,
                        "ORIGEM_INVALIDA",
                        "Solicitação não autorizada.");
                return;
            }
        }
        if (req.getRequestURI().equals("/api/auth/entrar") && !permitir(req.getRemoteAddr())) {
            res.setHeader("Retry-After", "60");
            problemas.escrever(
                    req,
                    res,
                    HttpStatus.TOO_MANY_REQUESTS,
                    "TENTATIVAS_EXCEDIDAS",
                    "Aguarde um minuto antes de tentar novamente.");
            return;
        }
        chain.doFilter(req, res);
    }

    private boolean mesmaOrigemDoTunnel(HttpServletRequest req, String receivedOrigin) {
        if (receivedOrigin == null) return false;
        try {
            URI source = URI.create(receivedOrigin);
            if (!"https".equals(source.getScheme())
                    || source.getHost() == null
                    || !source.getHost().matches("[a-z0-9-]+\\.([a-z0-9-]+\\.)?devtunnels\\.ms")
                    || source.getUserInfo() != null
                    || source.getQuery() != null
                    || source.getFragment() != null
                    || !source.getRawPath().isEmpty()
                    || (source.getPort() != -1 && source.getPort() != 443)) return false;
            // Comparacao exata com o destino desta requisicao; nao libera outro tunnel.
            String authority = source.getRawAuthority();
            return authority.equalsIgnoreCase(req.getHeader("Host"))
                    || ("https".equals(req.getHeader("X-Forwarded-Proto"))
                            && authority.equalsIgnoreCase(req.getHeader("X-Forwarded-Host")));
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private synchronized boolean permitir(String ip) {
        long agora = System.nanoTime();
        janelas.entrySet().removeIf(e -> agora - e.getValue().inicio() >= 60_000_000_000L);
        var old = janelas.get(ip);
        if (old == null && janelas.size() >= 4096) return false;
        if (old != null && old.chamadas() >= 20) return false;
        janelas.put(
                ip,
                new Janela(
                        old == null ? agora : old.inicio(), old == null ? 1 : old.chamadas() + 1));
        return true;
    }
}
