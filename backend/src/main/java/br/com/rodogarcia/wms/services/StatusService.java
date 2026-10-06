package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.dto.StatusResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class StatusService {

    private final Clock clock;

    public StatusService(Clock clock) {
        this.clock = clock;
    }

    public StatusResponse consultar() {
        return new StatusResponse("wms-rodogarcia", "DISPONIVEL", Instant.now(clock));
    }
}
