package br.com.rodogarcia.wms.config;

import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
@EnableConfigurationProperties(IdentidadeProperties.class)
public class CadastrosSegurancaConfig {
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    @ConditionalOnProperty(name = "wms.auth.enabled", havingValue = "false", matchIfMissing = true)
    JwtDecoder jwtDecoder(IdentidadeProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(properties.issuer()),
                        new JwtWmsValidator(properties.audience())));
        return decoder;
    }

    @Bean
    SecurityFilterChain cadastrosSecurity(HttpSecurity http, ProblemasApi problemas)
            throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {
                    if (Boolean.TRUE.equals(jwt.getClaim("trocar_senha"))) return List.of();
                    String perfil = jwt.getClaimAsString("wms_perfil");
                    return JwtWmsValidator.PERFIS.contains(perfil)
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + perfil))
                            : List.of();
                });
        var naoAutenticado =
                (org.springframework.security.web.AuthenticationEntryPoint)
                        (request, response, ex) -> {
                            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
                            problemas.escrever(
                                    request,
                                    response,
                                    HttpStatus.UNAUTHORIZED,
                                    "NAO_AUTENTICADO",
                                    "Autenticação necessária.");
                        };
        var negado =
                (org.springframework.security.web.access.AccessDeniedHandler)
                        (request, response, ex) ->
                                problemas.escrever(
                                        request,
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "ACESSO_NEGADO",
                                        "Acesso não autorizado.");
        return http
                // Esta API aceita apenas Bearer no cabeçalho; cookies não autenticam solicitações.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        a ->
                                a.dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/v1/status")
                                        .permitAll()
                                        .requestMatchers("/api/v1/auditoria/**")
                                        .hasRole("GESTOR")
                                        .requestMatchers("/api/v1/importacoes-enderecos/**")
                                        .hasRole("GESTOR")
                                        .requestMatchers(
                                                "/api/v1/servicos-cobranca/**",
                                                "/api/v1/tabelas-cobranca/**",
                                                "/api/v1/vinculos-tabela/**",
                                                "/api/v1/contratos-cobranca/**",
                                                "/api/v1/fatos-servico/**",
                                                "/api/v1/avarias/**",
                                                "/api/v1/calculos-cobranca/**",
                                                "/api/v1/fechamentos-cobranca/**",
                                                "/api/v1/ajustes-fechamento/**")
                                        .hasAnyRole("GESTOR", "SUPERVISOR")
                                        .requestMatchers(
                                                "/api/v1/clientes/**",
                                                "/api/v1/armazens/**",
                                                "/api/v1/produtos/**",
                                                "/api/v1/embalagens/**",
                                                "/api/v1/pedidos-entrada/**",
                                                "/api/v1/pedidos-saida/**",
                                                "/api/v1/unidades-logisticas/**",
                                                "/api/v1/contagens/**",
                                                "/api/v1/cargas-iniciais/**",
                                                "/api/v1/contingencias/**",
                                                "/api/v1/indicadores-estoque/**",
                                                "/api/v1/avisos-validade/**",
                                                "/api/v1/encerramentos/**",
                                                "/api/v1/estoque/**",
                                                "/api/v1/conjuntos-posicoes/**",
                                                "/api/v1/enderecos/**")
                                        .hasAnyRole("GESTOR", "SUPERVISOR", "OPERACAO")
                                        .anyRequest()
                                        .denyAll())
                .oauth2ResourceServer(
                        o ->
                                o.jwt(j -> j.jwtAuthenticationConverter(converter))
                                        .authenticationEntryPoint(naoAutenticado)
                                        .accessDeniedHandler(negado))
                .exceptionHandling(
                        e -> e.authenticationEntryPoint(naoAutenticado).accessDeniedHandler(negado))
                .build();
    }
}
