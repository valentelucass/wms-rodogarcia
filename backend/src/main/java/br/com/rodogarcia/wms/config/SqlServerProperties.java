package br.com.rodogarcia.wms.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("wms.database")
public record SqlServerProperties(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9.-]+") String host,
        @Min(1) @Max(65535) int port,
        @NotBlank @Pattern(regexp = "WMS_DEV|WMS_PROD") String name,
        @NotBlank String user,
        @NotBlank String password,
        @NotBlank String confirmedTarget,
        @NotBlank String confirmedServer) {
    public void validarAlvo() {
        if (host == null
                || !host.matches("[A-Za-z0-9.-]+")
                || port < 1
                || port > 65535
                || name == null
                || !name.equals("WMS_DEV")
                || user == null
                || user.isBlank()
                || password == null
                || password.isBlank()
                || confirmedServer == null
                || confirmedServer.isBlank()
                || confirmedServer.length() > 128
                || !confirmedServer.equals(confirmedServer.strip())
                || confirmedServer.chars().anyMatch(Character::isISOControl)
                || !(host + ":" + port + "/" + name).equals(confirmedTarget)) {
            throw new IllegalStateException(
                    "Confirme explicitamente o alvo SQL Server de desenvolvimento do WMS.");
        }
    }

    public void validarAlvoProducao() {
        if (host == null
                || !host.matches("[A-Za-z0-9.-]+")
                || port < 1
                || port > 65535
                || !"WMS_PROD".equals(name)
                || !"WMSPROD".equals(user)
                || password == null
                || password.isBlank()
                || confirmedServer == null
                || confirmedServer.isBlank()
                || confirmedServer.length() > 128
                || !confirmedServer.equals(confirmedServer.strip())
                || confirmedServer.chars().anyMatch(Character::isISOControl)
                || !(host + ":" + port + "/WMS_PROD").equals(confirmedTarget)) {
            throw new IllegalStateException(
                    "Confirme o alvo exclusivo WMS_PROD e a identidade restrita WMSPROD.");
        }
    }

    @Override
    public String toString() {
        return "SqlServerProperties[configuracao protegida]";
    }
}
