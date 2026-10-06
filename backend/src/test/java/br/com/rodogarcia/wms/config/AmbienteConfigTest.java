package br.com.rodogarcia.wms.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AmbienteConfigTest {

    @Test
    void permitePerfilLocalPadrao() {
        MockEnvironment environment = new MockEnvironment();
        environment.setDefaultProfiles("local");
        assertThatCode(() -> new AmbienteConfig(environment)).doesNotThrowAnyException();
    }

    @Test
    void impedePerfilOperacionalAindaNaoPreparado() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        assertThatThrownBy(() -> new AmbienteConfig(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("somente um perfil");
    }

    @Test
    void impedeMisturarTesteEOutroAmbiente() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test", "local");
        assertThatThrownBy(() -> new AmbienteConfig(environment))
                .isInstanceOf(IllegalStateException.class);
    }
}
