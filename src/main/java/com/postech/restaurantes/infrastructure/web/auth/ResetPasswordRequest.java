package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import com.postech.restaurantes.infrastructure.web.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/** Redefinição de senha com o token recebido por e-mail. */
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @ValidPassword String newPassword,
        @NotBlank String confirmPassword) {

    public ResetPasswordDTO toDTO() {
        return new ResetPasswordDTO(token, newPassword, confirmPassword);
    }
}
