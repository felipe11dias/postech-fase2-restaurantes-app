package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo da troca de senha. A confirmação é comparada pelo caso de uso, não aqui: "as duas
 * senhas conferem" é regra, e regra não mora na borda HTTP.
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 72) String newPassword,
        @NotBlank String confirmPassword) {

    public ChangePasswordDTO toDTO() {
        return new ChangePasswordDTO(currentPassword, newPassword, confirmPassword);
    }
}
