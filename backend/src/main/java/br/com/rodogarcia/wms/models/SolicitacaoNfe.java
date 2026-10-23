package br.com.rodogarcia.wms.models;

import java.time.Instant;

/** Retrato persistido de uma intenção fiscal. Não movimenta estoque. */
public record SolicitacaoNfe(
        String id,
        String planoId,
        String operacaoId,
        long pedidoId,
        String grupo,
        String natureza,
        long versaoPedido,
        String hashConfirmacao,
        String corpoJson,
        String documentoJson,
        String emitenteCnpj,
        int ambiente,
        String situacao,
        String notazzId,
        Long documentoId,
        Long documentoOrigemId,
        String numero,
        String serie,
        String chaveAcesso,
        String protocolo,
        String xml,
        String mensagem,
        String usuario,
        Instant criadaEm,
        Instant atualizadaEm) {
    public boolean pendente() {
        return !situacao.equals("AUTORIZADA") && !situacao.equals("CANCELADA");
    }
}
