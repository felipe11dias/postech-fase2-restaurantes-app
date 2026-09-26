package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.infrastructure.web.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da troca de senha. A confirmação é comparada pelo caso de uso, não aqui: "as duas
 * senhas conferem" é regra, e regra não mora na borda HTTP.
 */
public record ChangePasswordRequest(
        @Schema(example = "cliente12345") @NotBlank String currentPassword,
        @Schema(description = "De 8 caracteres a 72 bytes em UTF-8 (letras acentuadas ocupam 2 bytes)",
                example = "novaSenha456")
        @NotBlank @ValidPassword String newPassword,
        @Schema(description = "Repetição da senha nova", example = "novaSenha456")
        @NotBlank String confirmPassword) {

    public ChangePasswordDTO toDTO() {
        return new ChangePasswordDTO(currentPassword, newPassword, confirmPassword);
    }
}
