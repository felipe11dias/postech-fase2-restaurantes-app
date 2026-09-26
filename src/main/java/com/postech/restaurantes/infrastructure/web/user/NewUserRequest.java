package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.infrastructure.web.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
        @Schema(example = "João Silva") @NotBlank @Size(max = 150) String name,
        @Schema(example = "joao.silva@email.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "joao.silva") @NotBlank @Size(max = 50) String login,
        @Schema(description = "De 8 caracteres a 72 bytes em UTF-8 (letras acentuadas ocupam 2 bytes)",
                example = "senhaSegura123")
        @NotBlank @ValidPassword String password,
        @ArraySchema(arraySchema = @Schema(description = "Papéis pedidos. ROLE_ADMIN é recusado no autocadastro."),
                schema = @Schema(allowableValues = {"ROLE_OWNER", "ROLE_CUSTOMER"}, example = "ROLE_CUSTOMER"))
        @NotEmpty Set<String> roles,
        @Schema(description = "Opcional; pode ser vazio") @Valid List<AddressRequest> addresses) {

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
