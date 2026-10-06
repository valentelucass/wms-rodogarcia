package br.com.rodogarcia.wms.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("wms.identity")
public record IdentidadeProperties(
        @NotBlank @Pattern(regexp = "https://[^\\s]+") String issuer,
        @NotBlank @Pattern(regexp = "https://[^\\s]+") String jwkSetUri,
        @NotBlank String audience) {}
