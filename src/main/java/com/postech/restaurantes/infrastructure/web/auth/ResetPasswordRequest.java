package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.application.dto.auth.ResetPasswordDTO;
import com.postech.restaurantes.infrastructure.web.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Redefinição de senha com o token recebido por e-mail. */
public record ResetPasswordRequest(
        @Schema(description = "Token recebido por e-mail", example = "Qm9yZGVyU2VjcmV0VG9rZW5EZUV4ZW1wbG9fMzJieXRl")
        @NotBlank String token,
        @Schema(description = "De 8 caracteres a 72 bytes em UTF-8 (letras acentuadas ocupam 2 bytes)",
                example = "novaSenha456")
        @NotBlank @ValidPassword String newPassword,
        @Schema(description = "Repetição da senha nova", example = "novaSenha456")
        @NotBlank String confirmPassword) {

    public ResetPasswordDTO toDTO() {
        return new ResetPasswordDTO(token, newPassword, confirmPassword);
    }
}
