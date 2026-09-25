package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.infrastructure.web.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo da troca de senha. A confirmação é comparada pelo caso de uso, não aqui: "as duas
 * senhas conferem" é regra, e regra não mora na borda HTTP.
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @ValidPassword String newPassword,
        @NotBlank String confirmPassword) {

    public ChangePasswordDTO toDTO() {
        return new ChangePasswordDTO(currentPassword, newPassword, confirmPassword);
    }
}
