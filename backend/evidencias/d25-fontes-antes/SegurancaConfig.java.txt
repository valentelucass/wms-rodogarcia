package br.com.rodogarcia.wms.config;

import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "false", matchIfMissing = true)
public class SegurancaConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ProblemasApi problemas)
            throws Exception {
        return http.authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, "/api/v1/status")
                                        .permitAll()
                                        .anyRequest()
                                        .denyAll())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                                (request, response, exception) ->
                                                        problemas.escrever(
                                                                request,
                                                                response,
                                                                HttpStatus.FORBIDDEN,
                                                                "ACESSO_NEGADO",
                                                                "Acesso não disponível nesta etapa do WMS."))
                                        .accessDeniedHandler(
                                                (request, response, exception) ->
                                                        problemas.escrever(
                                                                request,
                                                                response,
                                                                HttpStatus.FORBIDDEN,
                                                                "ACESSO_NEGADO",
                                                                "Acesso não disponível nesta etapa do WMS.")))
                .build();
    }
}
