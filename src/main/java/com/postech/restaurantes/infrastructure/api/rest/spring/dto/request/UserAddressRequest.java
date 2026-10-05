package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.user.UserAddressDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Endereço do usuário como chega pelo HTTP: rótulo e marca de padrão, com o endereço aninhado —
 * a mesma forma de {@code user_addresses} apontando para {@code addresses}. O id é opcional e
 * identifica um endereço já existente do usuário, que é atualizado em vez de recriado.
 */
public record UserAddressRequest(
        @Schema(description = "Id de um endereço que o usuário já tem, para mantê-lo (o id não muda); "
                + "ausente cria um endereço novo. Os endereços que não vierem na lista são removidos.")
        UUID id,
        @Schema(description = "Opcional, ex.: Casa, Trabalho", example = "Casa") @Size(max = 50) String label,
        @Schema(description = "Opcional. Se nenhum endereço da lista for o padrão, o primeiro passa a ser; "
                + "mais de um é recusado.", example = "true")
        Boolean isDefault,
        @NotNull @Valid AddressRequest address) {

    public UserAddressDTO toDTO() {
        return new UserAddressDTO(id, label, isDefault, address == null ? null : address.toDTO());
    }

    public static List<UserAddressDTO> toDTOs(List<UserAddressRequest> requests) {
        return requests == null ? null : requests.stream().map(UserAddressRequest::toDTO).toList();
    }
}
