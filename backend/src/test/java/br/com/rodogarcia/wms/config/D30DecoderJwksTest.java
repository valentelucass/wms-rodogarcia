package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import br.com.rodogarcia.wms.exceptions.ProblemasApi;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.persistence.EntityManagerFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.web.client.RestTemplate;

/** Bean produtivo e JWKS interceptado em memória; nenhum servidor ou provedor externo. */
class D30DecoderJwksTest {
    private static final String ISSUER = "https://d30.invalid/issuer";
    private static final String JWKS = "https://d30.invalid/jwks";
    private static final String AUDIENCE = "wms-d30-local";

    @Test
    void selecionaBeanProdutivoEValidaAssinaturaIssuerAudienceComJwksFicticio() throws Exception {
        RSAKey chave = chave();
        Instant inicio = Instant.now().minusSeconds(5);
        String valido = token(chave, ISSUER, AUDIENCE, inicio);
        String issuerErrado = token(chave, "https://outro.invalid/issuer", AUDIENCE, inicio);
        String audienceErrada = token(chave, ISSUER, "outro-publico", inicio);
        String assinaturaErrada = token(chave(), ISSUER, AUDIENCE, inicio);
        String publico = new JWKSet(chave.toPublicJWK()).toString();
        AtomicInteger requisicoesInterceptadas = new AtomicInteger();
        try (var interceptacao =
                mockConstruction(
                        RestTemplate.class,
                        (rest, construcao) -> {
                            when(rest.exchange(any(RequestEntity.class), eq(String.class)))
                                    .thenAnswer(
                                            invocacao -> {
                                                RequestEntity<?> pedido = invocacao.getArgument(0);
                                                assertThat(pedido.getUrl().toString())
                                                        .isEqualTo(JWKS);
                                                assertThat(pedido.getMethod().name())
                                                        .isEqualTo("GET");
                                                requisicoesInterceptadas.incrementAndGet();
                                                return ResponseEntity.ok(publico);
                                            });
                        })) {
            contexto()
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class);
                                assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                                assertThat(context.getBeansOfType(EntityManagerFactory.class))
                                        .isEmpty();
                                JwtDecoder decoder = context.getBean(JwtDecoder.class);
                                assertThat(decoder).isInstanceOf(NimbusJwtDecoder.class);
                                assertThat(context.getBean("jwtDecoder")).isSameAs(decoder);
                                var resultado = decoder.decode(valido);
                                assertThat(resultado.getSubject()).isEqualTo("cedro-decoder-local");
                                assertThat(resultado.getIssuer().toString()).isEqualTo(ISSUER);
                                assertThat(resultado.getAudience()).containsExactly(AUDIENCE);
                                assertThat(resultado.getClaimAsString("wms_perfil"))
                                        .isEqualTo("GESTOR");
                                assertThat(resultado.getClaimAsStringList("wms_clientes"))
                                        .containsExactly("1");
                                assertThat(resultado.getClaimAsStringList("wms_armazens"))
                                        .containsExactly("2");
                                assertThat(resultado.getIssuedAt())
                                        .isEqualTo(Instant.ofEpochSecond(inicio.getEpochSecond()));
                                assertThat(resultado.getExpiresAt())
                                        .isEqualTo(
                                                Instant.ofEpochSecond(
                                                        inicio.plusSeconds(300).getEpochSecond()));
                                assertThatThrownBy(() -> decoder.decode(issuerErrado))
                                        .isInstanceOf(JwtException.class);
                                assertThatThrownBy(() -> decoder.decode(audienceErrada))
                                        .isInstanceOf(JwtException.class);
                                assertThatThrownBy(() -> decoder.decode(assinaturaErrada))
                                        .isInstanceOf(JwtException.class);
                                assertThat(requisicoesInterceptadas.get()).isPositive();
                            });
            assertThat(interceptacao.constructed()).isNotEmpty();
            assertThat(interceptacao.constructed())
                    .allMatch(cliente -> org.mockito.Mockito.mockingDetails(cliente).isMock());
        }
    }

    @Test
    void decoderJaFornecidoTemPreferenciaSemConstruirClienteJwks() throws Exception {
        JwtDecoder fornecido = mock(JwtDecoder.class);
        try (var interceptacao = mockConstruction(RestTemplate.class)) {
            contexto()
                    .withBean("decoderFornecido", JwtDecoder.class, () -> fornecido)
                    .run(
                            context -> {
                                assertThat(context).hasNotFailed().hasSingleBean(JwtDecoder.class);
                                assertThat(context.getBean(JwtDecoder.class)).isSameAs(fornecido);
                                assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                                assertThat(context.getBeansOfType(EntityManagerFactory.class))
                                        .isEmpty();
                            });
            assertThat(interceptacao.constructed()).isEmpty();
        }
    }

    @Test
    void cadastrosDesligadosNaoCriamDecoderOuDatasource() {
        try (var interceptacao = mockConstruction(RestTemplate.class)) {
            new ApplicationContextRunner()
                    .withUserConfiguration(CadastrosSegurancaConfig.class)
                    .withPropertyValues("wms.cadastros.enabled=false")
                    .run(
                            context -> {
                                assertThat(context)
                                        .hasNotFailed()
                                        .doesNotHaveBean(JwtDecoder.class);
                                assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
                                assertThat(context.getBeansOfType(EntityManagerFactory.class))
                                        .isEmpty();
                            });
            assertThat(interceptacao.constructed()).isEmpty();
        }
    }

    private ApplicationContextRunner contexto() throws Exception {
        HttpSecurity http = mock(HttpSecurity.class, Answers.RETURNS_SELF);
        when(http.build()).thenReturn(new DefaultSecurityFilterChain(AnyRequestMatcher.INSTANCE));
        return new ApplicationContextRunner()
                .withUserConfiguration(CadastrosSegurancaConfig.class)
                .withBean(HttpSecurity.class, () -> http)
                .withBean(ProblemasApi.class, () -> mock(ProblemasApi.class))
                .withPropertyValues(
                        "wms.cadastros.enabled=true",
                        "wms.identity.issuer=" + ISSUER,
                        "wms.identity.jwk-set-uri=" + JWKS,
                        "wms.identity.audience=" + AUDIENCE);
    }

    private static RSAKey chave() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var par = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) par.getPublic())
                .privateKey((RSAPrivateKey) par.getPrivate())
                .keyID("d30-ficticia")
                .build();
    }

    private static String token(RSAKey chave, String issuer, String audience, Instant inicio)
            throws Exception {
        var claims =
                new JWTClaimsSet.Builder()
                        .issuer(issuer)
                        .audience(audience)
                        .subject("cedro-decoder-local")
                        .issueTime(Date.from(inicio))
                        .expirationTime(Date.from(inicio.plusSeconds(300)))
                        .claim("wms_perfil", "GESTOR")
                        .claim("wms_clientes", List.of("1"))
                        .claim("wms_armazens", List.of("2"))
                        .build();
        var jwt =
                new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("d30-ficticia").build(),
                        claims);
        jwt.sign(new RSASSASigner(chave.toRSAPrivateKey()));
        return jwt.serialize();
    }
}
