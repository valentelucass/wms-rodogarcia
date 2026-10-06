package br.com.rodogarcia.wms.config;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Valida o contrato de identidade depois da assinatura, emissor e prazo do JWT. */
public class JwtWmsValidator implements OAuth2TokenValidator<Jwt> {
    public static final Set<String> PERFIS = Set.of("GESTOR", "SUPERVISOR", "OPERACAO");
    private final String audience;

    public JwtWmsValidator(String audience) {
        this.audience = audience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        Object perfil = jwt.getClaims().get("wms_perfil");
        Object sujeito = jwt.getClaims().get("sub");
        Instant inicio = jwt.getIssuedAt();
        Instant fim = jwt.getExpiresAt();
        if (!(perfil instanceof String p)
                || !PERFIS.contains(p)
                || !(sujeito instanceof String s)
                || s.isBlank()
                || s.length() > 200
                || jwt.getAudience() == null
                || !jwt.getAudience().contains(audience)
                || inicio == null
                || fim == null
                || !fim.isAfter(inicio)
                || Duration.between(inicio, fim).compareTo(Duration.ofMinutes(15)) > 0
                || inicio.isAfter(Instant.now().plusSeconds(60))
                || !escopoValido(jwt.getClaims().get("wms_clientes"))
                || !escopoValido(jwt.getClaims().get("wms_armazens"))) {
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token inválido para o WMS.", null));
        }
        return OAuth2TokenValidatorResult.success();
    }

    private boolean escopoValido(Object valor) {
        if (!(valor instanceof List<?> ids) || ids.size() > 500) {
            return false;
        }
        for (Object id : ids) {
            if (!(id instanceof String s) || !s.matches("[1-9][0-9]{0,18}")) {
                return false;
            }
            try {
                Long.parseLong(s);
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return true;
    }
}
