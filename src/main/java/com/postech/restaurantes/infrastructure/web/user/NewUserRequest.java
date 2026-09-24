package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.domain.entity.user.RoleName;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Corpo do autocadastro. A senha chega em claro e nunca sai daqui sem virar hash. */
public record NewUserRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 50) String login,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotEmpty Set<String> roles,
        @Valid List<AddressRequest> addresses) {

    /**
     * Os papéis são convertidos por {@code RoleName.from}, e não pela desserialização do
     * Jackson, para que um papel inexistente produza a mensagem do domínio em vez de um erro
     * de formato. Se {@code ROLE_ADMIN} pode ou não ser pedido aqui é decisão do caso de uso.
     */
    public NewUserDTO toDTO() {
        return new NewUserDTO(name, email, login, password, papeis(), AddressRequest.toDTOs(addresses));
    }

    private Set<RoleName> papeis() {
        return roles == null ? null : roles.stream().map(RoleName::from).collect(Collectors.toSet());
    }
}
