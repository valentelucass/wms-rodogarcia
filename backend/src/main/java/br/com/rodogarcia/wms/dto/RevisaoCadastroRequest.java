package br.com.rodogarcia.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevisaoCadastroRequest(
        @NotNull @Min(0) Long versao, @NotBlank @Size(min = 5, max = 500) String motivo) {}
