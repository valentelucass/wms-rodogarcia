package br.com.rodogarcia.wms.exceptions;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Somente mensagens controladas pela aplicação; nunca mensagens de SQL ou dados recebidos. */
public class RegraNegocioException extends RuntimeException {
    @Serial private static final long serialVersionUID = 1L;

    private final HttpStatus status;
    private final String codigo;

    public RegraNegocioException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public static RegraNegocioException conflito(String codigo, String mensagem) {
        return new RegraNegocioException(HttpStatus.CONFLICT, codigo, mensagem);
    }

    public static RegraNegocioException naoEncontrado() {
        return new RegraNegocioException(
                HttpStatus.NOT_FOUND, "CADASTRO_NAO_ENCONTRADO", "Cadastro não encontrado.");
    }
}
