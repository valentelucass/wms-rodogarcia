package br.com.rodogarcia.wms.exceptions;

import java.io.Serial;
import org.springframework.http.HttpStatus;

/** A falha é persistida para não desfazer bloqueios ou revogação por replay. */
public class FalhaLoginException extends RegraNegocioException {
    @Serial private static final long serialVersionUID = 1L;

    public FalhaLoginException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "ACESSO_INVALIDO",
                "Acesso inválido ou expirado. Confira os dados e tente novamente mais tarde.");
    }
}
