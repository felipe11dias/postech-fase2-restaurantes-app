package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Redefinição de senha com o token recebido por e-mail. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword,
        @NotBlank String confirmPassword) {

    public ResetPasswordDTO toDTO() {
        return new ResetPasswordDTO(token, newPassword, confirmPassword);
    }
}
