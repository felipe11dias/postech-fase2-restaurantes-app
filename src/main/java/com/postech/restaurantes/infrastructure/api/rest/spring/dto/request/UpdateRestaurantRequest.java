package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.UUID;

/** Corpo da requisição para atualização de restaurante. */
public record UpdateRestaurantRequest(
        @Schema(description = "ID do usuário dono do restaurante") @NotNull UUID userId,
        @Schema(description = "ID do endereço do restaurante") @NotNull UUID addressId,
        @Schema(example = "Restaurante Sabor & Arte") @NotBlank @Size(max = 150) String name,
        @Schema(example = "08:00:00") @NotNull LocalTime officeHourStart,
        @Schema(example = "22:00:00") @NotNull LocalTime officeHourEnd
) {

    public UpdateRestaurantDTO toDTO(UUID id) {
        return new UpdateRestaurantDTO(id, userId, addressId, name, officeHourStart, officeHourEnd);
    }
}
