package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import jakarta.validation.constraints.NotBlank;

/** Credenciais de login. */
public record LoginRequest(@NotBlank String login, @NotBlank String password) {

    public CredentialsDTO toDTO() {
        return new CredentialsDTO(login, password);
    }
}
