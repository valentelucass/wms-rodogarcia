package br.com.rodogarcia.wms.models;

/** A inativação definitiva depende da verificação de estoque, pedidos e cobranças (AC12). */
public enum SituacaoCadastro {
    ATIVO,
    ENCERRAMENTO_PENDENTE,
    INATIVO
}
