package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LoginPasswordEncoderTest {
    @Test
    void aceitaHashDoConfiguradorRealComSenhaFicticiaUtf8() throws Exception {
        String senha = "Fixture-configuração-123!";
        var process =
                new ProcessBuilder(
                                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                                Path.of("../infra/auth/PrepararLogin.java")
                                        .toAbsolutePath()
                                        .normalize()
                                        .toString())
                        .start();
        try {
            try (var input = process.outputWriter(StandardCharsets.UTF_8)) {
                input.write(senha);
                input.newLine();
            }
            String output =
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.getErrorStream().readAllBytes();
            assertThat(process.waitFor(30, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).isZero();
            String hash =
                    JsonMapper.builder().build().readTree(output).get("bootstrapHash").asString();
            var encoder = new LoginConfig().passwordEncoder();
            assertThat(encoder.matches(senha, hash)).isTrue();
            assertThat(encoder.matches(senha + "-incorreta", hash)).isFalse();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
