package br.com.rodogarcia.wms.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Chave efêmera somente para os testes: nunca é salva ou incluída no JAR. */
@TestConfiguration(proxyBeanMethods = false)
public class IdentidadeTesteConfig {
    @Bean
    RSAKey chaveTeste() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID("chave-efemera-teste")
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey chave, IdentidadeProperties properties) throws Exception {
        var decoder = NimbusJwtDecoder.withPublicKey(chave.toRSAPublicKey()).build();
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        JwtValidators.createDefaultWithIssuer(properties.issuer()),
                        new JwtWmsValidator(properties.audience())));
        return decoder;
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey chave) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(chave)));
    }
}
