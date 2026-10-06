package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da troca de status do entregador. O status vem como texto e é convertido pelo domínio, para
 * um valor desconhecido produzir a mensagem do domínio, e não um erro de formato.
 */
public record CourierStatusRequest(
        @Schema(allowableValues = {"OFFLINE", "AVAILABLE", "BUSY"}, example = "AVAILABLE") @NotBlank String status) {
}
