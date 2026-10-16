package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.infrastructure.api.rest.spring.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Corpo do autocadastro. A senha chega em claro e nunca sai daqui sem virar hash. Cada perfil é
 * opcional, mas ao menos um precisa vir — regra do domínio, que recusa com mensagem própria. Não há
 * perfil de administrador: ele não se obtém por autocadastro.
 */
public record NewUserRequest(
        @Schema(example = "João Silva") @NotBlank @Size(max = 150) String name,
        @Schema(example = "joao.silva@email.com") @NotBlank @Email @Size(max = 150) String email,
        @Schema(example = "joao.silva") @NotBlank @Size(max = 50) String login,
        @Schema(description = "De 8 caracteres a 72 bytes em UTF-8 (letras acentuadas ocupam 2 bytes)",
                example = "senhaSegura123")
        @NotBlank @ValidPassword String password,
        @Schema(description = "Perfil de dono de restaurante (opcional)") @Valid OwnerProfileRequest owner,
        @Schema(description = "Perfil de cliente (opcional)") @Valid ClientProfileRequest client,
        @Schema(description = "Perfil de entregador (opcional; com cliente, o CPF é o mesmo)")
        @Valid CourierProfileRequest courier,
        @Schema(description = "Opcional; pode ser vazio") @Valid List<UserAddressRequest> addresses) {

    public NewUserDTO toDTO() {
        return new NewUserDTO(name, email, login, password,
                owner == null ? null : owner.toDTO(),
                client == null ? null : client.toDTO(),
                courier == null ? null : courier.toDTO(),
                UserAddressRequest.toDTOs(addresses));
    }
}
