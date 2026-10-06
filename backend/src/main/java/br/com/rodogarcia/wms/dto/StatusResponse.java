package br.com.rodogarcia.wms.dto;

import java.time.Instant;

public record StatusResponse(String aplicacao, String status, Instant instante) {}
