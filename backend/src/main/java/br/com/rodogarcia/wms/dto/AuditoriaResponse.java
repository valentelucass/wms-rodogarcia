package br.com.rodogarcia.wms.dto;

import br.com.rodogarcia.wms.models.AuditoriaCadastro;
import java.time.Instant;

public record AuditoriaResponse(
        Long id,
        String tipo,
        Long registroId,
        String acao,
        String usuario,
        Instant instante,
        String idOperacao,
        String motivo,
        String dadosAntes,
        String dadosDepois) {
    public static AuditoriaResponse de(AuditoriaCadastro e) {
        return new AuditoriaResponse(
                e.getId(),
                e.getTipo(),
                e.getRegistroId(),
                e.getAcao(),
                e.getUsuario(),
                e.getInstante(),
                e.getIdOperacao(),
                e.getMotivo(),
                e.getDadosAntes(),
                e.getDadosDepois());
    }
}
