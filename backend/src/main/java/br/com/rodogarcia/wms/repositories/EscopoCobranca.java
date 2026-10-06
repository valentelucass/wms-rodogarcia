package br.com.rodogarcia.wms.repositories;

/** Projeção escalar: obtém contexto antes de carregar estado mutável e adquirir locks. */
public interface EscopoCobranca {
    Long getClienteId();

    Long getArmazemId();
}
