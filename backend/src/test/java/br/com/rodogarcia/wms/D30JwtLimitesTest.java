package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rodogarcia.wms.config.JwtWmsValidator;
import br.com.rodogarcia.wms.services.AcessoService;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Limites positivos independentes do contrato de identidade; não prova assinatura/provedor. */
class D30JwtLimitesTest {
    @ParameterizedTest
    @CsvSource({"900,12,1", "300,200,1", "300,12,500"})
    void aceitaLimitesPositivosEConservaLongMaxNoAlcance(
            int duracao, int caracteres, int quantidade) {
        List<String> ids =
                IntStream.range(0, quantidade)
                        .mapToObj(
                                i ->
                                        i == quantidade - 1
                                                ? "9223372036854775807"
                                                : Integer.toString(i + 1))
                        .toList();
        var token = token(duracao, "S".repeat(caracteres), ids);
        assertThat(new JwtWmsValidator("wms-testes").validate(token).hasErrors()).isFalse();
        var anterior = SecurityContextHolder.getContext();
        var local = SecurityContextHolder.createEmptyContext();
        local.setAuthentication(
                new JwtAuthenticationToken(
                        token, List.of(new SimpleGrantedAuthority("ROLE_SUPERVISOR"))));
        SecurityContextHolder.setContext(local);
        try {
            var acesso =
                    new AcessoService(
                            new org.springframework.beans.factory.support
                                            .DefaultListableBeanFactory()
                                    .getBeanProvider(
                                            br.com.rodogarcia.wms.services.LoginService.class));
            assertThat(acesso.usuario()).hasSize(caracteres);
            assertThat(acesso.clientes()).hasSize(quantidade).contains(9223372036854775807L);
            assertThat(acesso.armazens()).hasSize(quantidade).contains(9223372036854775807L);
            assertThat(acesso.clientes().getLast()).isEqualTo(9223372036854775807L);
            acesso.cliente(9223372036854775807L);
            acesso.armazem(9223372036854775807L);
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    @Test
    void vizinhoNovecentosEUmSegundosRecusado() {
        var token = token(901, "Sujeito-fictício", List.of("1"));
        var result = new JwtWmsValidator("wms-testes").validate(token);
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .extracting(e -> e.getErrorCode())
                .containsExactly("invalid_token");
    }

    private static Jwt token(int duracao, String sujeito, List<String> ids) {
        Instant inicio = Instant.now().minusSeconds(5);
        return Jwt.withTokenValue("token-ficticio-local-nao-assinado")
                .header("alg", "RS256")
                .subject(sujeito)
                .audience(List.of("wms-testes"))
                .issuedAt(inicio)
                .expiresAt(inicio.plusSeconds(duracao))
                .claim("wms_perfil", "SUPERVISOR")
                .claim("wms_clientes", ids)
                .claim("wms_armazens", ids)
                .build();
    }
}
