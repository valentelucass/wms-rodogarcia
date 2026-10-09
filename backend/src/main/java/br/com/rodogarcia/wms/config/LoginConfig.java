package br.com.rodogarcia.wms.config;

import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import br.com.rodogarcia.wms.services.LoginService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "wms.auth.enabled", havingValue = "true")
public class LoginConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder(
                "pbkdf2-600k",
                Map.of(
                        "pbkdf2-600k",
                        new Pbkdf2PasswordEncoder(
                                "",
                                16,
                                600000,
                                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm
                                        .PBKDF2WithHmacSHA256)));
    }

    @Bean
    RSAKey chaveLogin(@Value("${wms.auth.private-key}") String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            var factory = KeyFactory.getInstance("RSA");
            var key = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(bytes));
            java.util.Arrays.fill(bytes, (byte) 0);
            if (key.getModulus().bitLength() < 3072) throw new IllegalArgumentException();
            var pub =
                    (RSAPublicKey)
                            factory.generatePublic(
                                    new RSAPublicKeySpec(
                                            key.getModulus(), key.getPublicExponent()));
            String kid =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256").digest(pub.getEncoded()));
            return new RSAKey.Builder(pub).privateKey(key).keyID(kid).build();
        } catch (Exception ex) {
            throw new IllegalStateException("AUTH_CHAVE_PROTEGIDA_INVALIDA");
        }
    }

    @Bean
    JwtEncoder loginEncoder(RSAKey chaveLogin) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(chaveLogin)));
    }

    @Bean
    JwtDecoder loginDecoder(RSAKey chaveLogin, IdentidadeProperties identidade, LoginService login)
            throws Exception {
        var decoder = NimbusJwtDecoder.withPublicKey(chaveLogin.toRSAPublicKey()).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(identidade.issuer()),
                        new JwtWmsValidator(identidade.audience()),
                        jwt ->
                                login.jwtAtivo(jwt)
                                        ? OAuth2TokenValidatorResult.success()
                                        : OAuth2TokenValidatorResult.failure(
                                                new OAuth2Error(
                                                        "invalid_token",
                                                        "Sessão encerrada.",
                                                        null))));
        return decoder;
    }

    @Bean
    ApplicationRunner principalLogin(
            LoginService login, @Value("${wms.auth.bootstrap-hash:}") String hash) {
        return args -> login.iniciarPrincipal(hash);
    }

    @Bean
    @Order(1)
    SecurityFilterChain loginSecurity(
            HttpSecurity http,
            ProblemasApi problemas,
            @Value("${wms.auth.origin}") String origin,
            @Value("${wms.auth.proxy-origin:}") String proxyOrigin,
            @Value("${wms.auth.secure-cookie:true}") boolean secure,
            @Value("${server.address:127.0.0.1}") String address,
            org.springframework.core.env.Environment environment)
            throws Exception {
        var uri = java.net.URI.create(origin);
        boolean local =
                !secure
                        && "http".equals(uri.getScheme())
                        && "127.0.0.1".equals(uri.getHost())
                        && "127.0.0.1".equals(address)
                        && environment.acceptsProfiles(
                                org.springframework.core.env.Profiles.of("sqlserver-dev", "test"));
        boolean dynamicTunnel = origin.isEmpty() && !proxyOrigin.isEmpty();
        if (!dynamicTunnel
                && ((!secure && !local)
                        || (secure && !"https".equals(uri.getScheme()))
                        || uri.getUserInfo() != null
                        || uri.getQuery() != null
                        || uri.getFragment() != null
                        || (uri.getPath() != null && !uri.getPath().isEmpty())
                        || uri.getHost() == null)) {
            throw new IllegalStateException("AUTH_ORIGIN_TLS_INVALIDA");
        }
        if (!proxyOrigin.isEmpty()) {
            var proxy = java.net.URI.create(proxyOrigin);
            if (!secure
                    || !"127.0.0.1".equals(address)
                    || !environment.acceptsProfiles(
                            org.springframework.core.env.Profiles.of("sqlserver-dev", "test"))
                    || !"http".equals(proxy.getScheme())
                    || !"localhost".equals(proxy.getHost())
                    || proxy.getPort() < 1024
                    || proxy.getPort() > 65535
                    || !proxyOrigin.equals("http://localhost:" + proxy.getPort())) {
                throw new IllegalStateException("AUTH_PROXY_ORIGIN_INVALIDA");
            }
        }
        var csrf = new CookieCsrfTokenRepository();
        csrf.setCookieCustomizer(
                c -> c.httpOnly(true).secure(secure).sameSite("Strict").path("/api/auth"));
        return http.securityMatcher("/api/auth/**")
                .csrf(c -> c.csrfTokenRepository(csrf))
                .addFilterBefore(
                        new LoginProtecaoFilter(origin, proxyOrigin, problemas), CsrfFilter.class)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                "/api/auth/csrf",
                                                "/api/auth/jwks",
                                                "/api/auth/entrar",
                                                "/api/auth/renovar",
                                                "/api/auth/sair")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        o ->
                                o.jwt(j -> {})
                                        .authenticationEntryPoint(
                                                (req, res, ex) ->
                                                        problemas.escrever(
                                                                req,
                                                                res,
                                                                HttpStatus.UNAUTHORIZED,
                                                                "NAO_AUTENTICADO",
                                                                "Entre novamente no WMS.")))
                .exceptionHandling(
                        e ->
                                e.accessDeniedHandler(
                                        (req, res, ex) ->
                                                problemas.escrever(
                                                        req,
                                                        res,
                                                        HttpStatus.FORBIDDEN,
                                                        "ACESSO_NEGADO",
                                                        "Solicitação não autorizada.")))
                .build();
    }
}
