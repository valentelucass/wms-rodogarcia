package br.com.rodogarcia.wms.dto;

/** Descreve a restrição violada sem repetir o valor enviado pelo solicitante. */
public record ErroCampoResponse(String campo, String codigo) {}
