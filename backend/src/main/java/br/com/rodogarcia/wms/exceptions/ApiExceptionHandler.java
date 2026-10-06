package br.com.rodogarcia.wms.exceptions;

import br.com.rodogarcia.wms.config.IdentificacaoOperacaoFilter;
import br.com.rodogarcia.wms.dto.ErroCampoResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final ProblemasApi problemas;

    public ApiExceptionHandler(ProblemasApi problemas) {
        this.problemas = problemas;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String codigo = "HTTP_" + status.value();
        String mensagem =
                switch (status.value()) {
                    case 400 -> "Solicitação inválida. Confira os dados enviados.";
                    case 404 -> "Recurso não encontrado.";
                    case 405 -> "Método não permitido para este recurso.";
                    case 406 -> "Formato de resposta não disponível.";
                    case 415 -> "Formato de conteúdo não suportado.";
                    default -> "Não foi possível processar a solicitação.";
                };
        ProblemDetail problema = problemas.criar(status, codigo, mensagem, idOperacao(request));
        if (exception instanceof MethodArgumentNotValidException validacao) {
            problema.setType(java.net.URI.create("urn:wms:erro:DADOS_INVALIDOS"));
            problema.setProperty("codigo", "DADOS_INVALIDOS");
            List<ErroCampoResponse> campos =
                    validacao.getBindingResult().getFieldErrors().stream()
                            .map(erro -> new ErroCampoResponse(erro.getField(), erro.getCode()))
                            .distinct()
                            .toList();
            problema.setProperty("campos", campos);
        }
        HttpHeaders respostaHeaders = new HttpHeaders();
        respostaHeaders.putAll(headers);
        respostaHeaders.setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        return super.handleExceptionInternal(exception, problema, respostaHeaders, status, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> tratarFalhaInesperada(Exception exception, WebRequest request) {
        // Não registrar mensagem/stacktrace que possa conter SQL, credenciais ou payloads.
        LOG.error(
                "Falha interna; tipo={}; idOperacao={}",
                exception.getClass().getSimpleName(),
                idOperacao(request));
        ProblemDetail problema =
                problemas.criar(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "ERRO_INTERNO",
                        "Ocorreu uma falha interna. Informe o identificador da operação ao suporte.",
                        idOperacao(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problema);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Object> tratarRegra(RegraNegocioException exception, WebRequest request) {
        return resposta(
                exception.getStatus(), exception.getCodigo(), exception.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> tratarAcessoNegado(WebRequest request) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso não autorizado.", request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> tratarValidacaoDoServico(WebRequest request) {
        return resposta(
                HttpStatus.BAD_REQUEST,
                "DADOS_INVALIDOS",
                "Solicitação inválida. Confira os dados enviados.",
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> tratarIntegridade(WebRequest request) {
        LOG.warn("Conflito de integridade; idOperacao={}", idOperacao(request));
        return resposta(
                HttpStatus.CONFLICT,
                "CONFLITO_DE_INTEGRIDADE",
                "Cadastro duplicado ou vínculo incompatível. Confira os dados antes de tentar novamente.",
                request);
    }

    @ExceptionHandler({
        OptimisticLockingFailureException.class,
        PessimisticLockingFailureException.class
    })
    public ResponseEntity<Object> tratarConcorrencia(WebRequest request) {
        return resposta(
                HttpStatus.CONFLICT,
                "CONFLITO_CONCORRENTE",
                "Outra operação alterou ou está alterando este cadastro. Consulte novamente antes de confirmar.",
                request);
    }

    private ResponseEntity<Object> resposta(
            HttpStatus status, String codigo, String mensagem, WebRequest request) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problemas.criar(status, codigo, mensagem, idOperacao(request)));
    }

    private String idOperacao(WebRequest request) {
        return (String)
                request.getAttribute(
                        IdentificacaoOperacaoFilter.ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
    }
}
