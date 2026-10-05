package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

/** Corpo da requisição para atualização de restaurante. */
public record UpdateRestaurantRequest(
        @Schema(description = "ID do usuário dono do restaurante") @NotNull UUID userId,
        @Schema(description = "Endereço do próprio restaurante (não é escolhido entre os do dono)")
        @NotNull @Valid AddressRequest address,
        @Schema(example = "Restaurante Sabor & Arte") @NotBlank @Size(max = 150) String name,
        @Schema(example = "08:00:00") @NotNull LocalTime officeHourStart,
        @Schema(example = "22:00:00") @NotNull LocalTime officeHourEnd
) {

    public UpdateRestaurantDTO toDTO(UUID id) {
        return new UpdateRestaurantDTO(id, userId, address == null ? null : address.toDTO(), name, officeHourStart, officeHourEnd);
    }
}
