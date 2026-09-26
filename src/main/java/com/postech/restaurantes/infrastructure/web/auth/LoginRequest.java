package com.postech.restaurantes.infrastructure.web.auth;

import com.postech.restaurantes.application.dto.auth.CredentialsDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Credenciais de login. Os exemplos são do usuário de demonstração criado pela migration V2. */
public record LoginRequest(
        @Schema(example = "admin.demo") @NotBlank String login,
        @Schema(example = "admin12345") @NotBlank String password) {

    public CredentialsDTO toDTO() {
        return new CredentialsDTO(login, password);
    }
}
