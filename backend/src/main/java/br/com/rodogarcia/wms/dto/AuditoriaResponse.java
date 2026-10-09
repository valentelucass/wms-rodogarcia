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
        String dadosDepois,
        String tipoFisico) {
    public AuditoriaResponse(
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
        this(
                id,
                tipo,
                registroId,
                acao,
                usuario,
                instante,
                idOperacao,
                motivo,
                dadosAntes,
                dadosDepois,
                tipo);
    }

    public static AuditoriaResponse de(AuditoriaCadastro e) {
        return de(e, e.getTipo());
    }

    public static AuditoriaResponse de(AuditoriaCadastro e, String tipoLogico) {
        return new AuditoriaResponse(
                e.getId(),
                tipoLogico,
                e.getRegistroId(),
                e.getAcao(),
                e.getUsuario(),
                e.getInstante(),
                e.getIdOperacao(),
                e.getMotivo(),
                e.getDadosAntes(),
                e.getDadosDepois(),
                e.getTipo());
    }
}
