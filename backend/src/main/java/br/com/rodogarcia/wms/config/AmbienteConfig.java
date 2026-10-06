package br.com.rodogarcia.wms.config;

import java.time.Clock;
import java.util.Arrays;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class AmbienteConfig {

    public AmbienteConfig(Environment environment) {
        String[] ativos = environment.getActiveProfiles();
        String[] perfis = ativos.length == 0 ? environment.getDefaultProfiles() : ativos;
        if (perfis.length != 1
                || Arrays.stream(perfis)
                        .anyMatch(p -> !Set.of("local", "test", "sqlserver-dev").contains(p))) {
            throw new IllegalStateException(
                    "Esta base permite somente um perfil: local, test ou sqlserver-dev. "
                            + "Outros ambientes dependem da preparação de implantação do WMS.");
        }
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
