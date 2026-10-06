package br.com.rodogarcia.wms.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Compartilha a ordem temporal dos fixtures e serviços somente neste contexto de teste. */
@TestConfiguration(proxyBeanMethods = false)
public class RelogioExpedicaoTesteConfig {
    @Bean
    @Primary
    Clock clockExpedicao() {
        return new RelogioMonotonico(ZoneOffset.UTC, new AtomicReference<>(Instant.EPOCH));
    }

    private static final class RelogioMonotonico extends Clock {
        private final ZoneId zona;
        private final AtomicReference<Instant> ultimo;

        private RelogioMonotonico(ZoneId zona, AtomicReference<Instant> ultimo) {
            this.zona = zona;
            this.ultimo = ultimo;
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return new RelogioMonotonico(zona, ultimo);
        }

        @Override
        public Instant instant() {
            return ultimo.updateAndGet(
                    anterior -> {
                        Instant agora = Instant.now().truncatedTo(ChronoUnit.MICROS);
                        return agora.isAfter(anterior) ? agora : anterior.plusNanos(1000);
                    });
        }
    }
}
