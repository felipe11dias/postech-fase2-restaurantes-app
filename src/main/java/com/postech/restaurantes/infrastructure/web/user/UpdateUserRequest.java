package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Corpo da atualização cadastral. Sem senha: a troca tem endpoint e caso de uso próprios. */
public record UpdateUserRequest(
        @Schema(example = "João Silva") @NotBlank @Size(max = 150) String name,
        @Schema(example = "joao.silva@email.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "joao.silva") @NotBlank @Size(max = 50) String login,
        @Schema(description = "Substitui a lista inteira; ausente ou vazia remove todos os endereços")
        @Valid List<AddressRequest> addresses) {

    public UpdateUserDTO toDTO() {
        return new UpdateUserDTO(name, email, login, AddressRequest.toDTOs(addresses));
    }
}
