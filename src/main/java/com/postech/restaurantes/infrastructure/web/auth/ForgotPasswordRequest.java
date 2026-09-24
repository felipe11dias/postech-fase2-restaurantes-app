package com.postech.restaurantes.infrastructure.web.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Pedido de recuperação de senha. */
public record ForgotPasswordRequest(@NotBlank @Email String email) {
}
