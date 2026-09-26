package com.postech.restaurantes.infrastructure.web.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Pedido de recuperação de senha. */
public record ForgotPasswordRequest(
        @Schema(example = "cliente.demo@email.com") @NotBlank @Email String email) {
}
