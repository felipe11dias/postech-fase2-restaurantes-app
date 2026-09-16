package com.postech.restaurantes.application.dto;

/** Entrada da redefinição de senha via token recebido por e-mail. */
public record ResetPasswordDTO(String token, String newPassword, String confirmPassword) {
}
