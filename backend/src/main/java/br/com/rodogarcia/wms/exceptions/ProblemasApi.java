package br.com.rodogarcia.wms.exceptions;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProblemasApi {

    private final JsonMapper mapper;

    public ProblemasApi(JsonMapper mapper) {
        this.mapper = mapper;
    }

    public ProblemDetail criar(
            HttpStatusCode status, String codigo, String mensagem, String idOperacao) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setTitle("Não foi possível concluir a solicitação");
        problema.setType(URI.create("urn:wms:erro:" + codigo));
        // Não incluir a URL, pois identificadores e parâmetros podem conter dados sensíveis.
        problema.setInstance(URI.create("urn:uuid:" + idOperacao));
        problema.setProperty("codigo", codigo);
        problema.setProperty("idOperacao", idOperacao);
        return problema;
    }

    public void escrever(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatusCode status,
            String codigo,
            String mensagem)
            throws IOException {
        String idOperacao = (String) request.getAttribute(IdentificacaoOperacaoFilter.ATRIBUTO);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), criar(status, codigo, mensagem, idOperacao));
    }
}
