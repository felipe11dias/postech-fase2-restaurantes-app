package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Corpo da atualização cadastral. Sem senha: a troca tem endpoint e caso de uso próprios. */
public record UpdateUserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 50) String login,
        @Valid List<AddressRequest> addresses) {

    public UpdateUserDTO toDTO() {
        return new UpdateUserDTO(name, email, login, AddressRequest.toDTOs(addresses));
    }
}
