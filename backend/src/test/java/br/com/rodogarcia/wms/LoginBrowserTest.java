package br.com.rodogarcia.wms;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/** Navegador real + HTTP + JPA, exclusivamente H2 efêmero; nunca SQL Server. */
@EnabledIfSystemProperty(named = "wms.browser.tests", matches = "true")
@ActiveProfiles("test")
@TestPropertySource(
        locations = "classpath:cadastros-test.properties",
        properties = {
            "spring.datasource.url=jdbc:h2:mem:wms-login-browser-d32;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;INIT=CREATE SCHEMA IF NOT EXISTS wms",
            "wms.auth.enabled=true",
            "wms.auth.origin=http://127.0.0.1:59999",
            "wms.auth.secure-cookie=false"
        })
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginBrowserTest {
    @Autowired Environment environment;

    @DynamicPropertySource
    static void materialFicticio(DynamicPropertyRegistry registry) throws Exception {
        LoginIntegrationTest.chaves(registry);
    }

    @Test
    void navegadorExecutaLoginAdministracaoETrocasObrigatorias() throws Exception {
        assertThat(environment.getProperty("spring.datasource.url"))
                .startsWith("jdbc:h2:mem:wms-login-browser-d32");
        Path root = Path.of("..").toAbsolutePath().normalize();
        Path output = root.resolve("orchestracao/.runtime/login-d32");
        Files.createDirectories(output);
        var process =
                new ProcessBuilder(
                                "node.exe",
                                root.resolve("frontend/tools/login-browser.mjs").toString(),
                                environment.getProperty("local.server.port"))
                        .directory(root.resolve("frontend").toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(output.resolve("browser.log").toFile())
                        .start();
        try {
            assertThat(process.waitFor(120, TimeUnit.SECONDS))
                    .as("Prazo do navegador local")
                    .isTrue();
            assertThat(process.exitValue())
                    .as("Ver browser.log sanitizado, sem credenciais")
                    .isZero();
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }
}
