package br.com.rodogarcia.wms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentificacaoOperacaoFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String ATRIBUTO = "idOperacao";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Gerado no servidor: cabeçalhos do solicitante não entram no contexto de logs.
        String idOperacao = UUID.randomUUID().toString();
        request.setAttribute(ATRIBUTO, idOperacao);
        response.setHeader(HEADER, idOperacao);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(ATRIBUTO, idOperacao)) {
            filterChain.doFilter(request, response);
        }
    }
}
