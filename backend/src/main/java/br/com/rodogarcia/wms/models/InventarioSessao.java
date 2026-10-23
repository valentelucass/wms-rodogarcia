package br.com.rodogarcia.wms.models;

import java.time.Instant;

/** Estado persistido pelo repositório SQL; não altera as entidades de estoque. */
public record InventarioSessao(
        Long id,
        long versao,
        Long clienteId,
        Long armazemId,
        TipoInventario tipo,
        Long produtoId,
        Long enderecoId,
        String situacao,
        Instant iniciadoEm,
        Instant encerradoEm,
        String usuario,
        String motivo,
        long unidadesLidas,
        Integer divergencias) {}
