package br.com.rodogarcia.wms.services;

import br.com.rodogarcia.wms.repositories.TarefaColetorRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name = "wms.cadastros.enabled", havingValue = "true")
public class TarefaColetorRegistro {
    private final TarefaColetorRepository tarefas;
    private final Clock clock;
    private final boolean habilitado;

    public TarefaColetorRegistro(
            TarefaColetorRepository tarefas,
            Clock clock,
            @Value("${wms.coletor-tarefas.enabled:false}") boolean habilitado) {
        this.tarefas = tarefas;
        this.clock = clock;
        this.habilitado = habilitado;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(String tipo, Long origem, Long cliente, Long armazem) {
        if (habilitado)
            tarefas.registrar(
                    tipo, origem, cliente, armazem, clock.instant().truncatedTo(ChronoUnit.MICROS));
    }
}
