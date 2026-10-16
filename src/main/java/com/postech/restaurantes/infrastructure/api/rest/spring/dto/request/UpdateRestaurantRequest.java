package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Corpo da requisição para atualização de restaurante. Sem {@code userId}, o dono continua o mesmo; trocar
 * o dono é operação de administrador (regra de posse, no {@code @PreAuthorize}).
 */
public record UpdateRestaurantRequest(
        @Schema(description = "Novo dono. Opcional: ausente, o dono continua o mesmo; trocar, só administrador")
        UUID userId,
        @Schema(description = "Endereço do próprio restaurante (não é escolhido entre os do dono)")
        @NotNull @Valid AddressRequest address,
        @Schema(example = "Restaurante Sabor & Arte") @NotBlank @Size(max = 150) String name,
        @Schema(description = "Horários de funcionamento por dia da semana; ao menos um, sem sobreposição")
        @NotNull @Valid List<OfficeHourRequest> officeHours
) {

    public UpdateRestaurantDTO toDTO(UUID id) {
        return new UpdateRestaurantDTO(id, userId, address == null ? null : address.toDTO(), name,
                OfficeHourRequest.toDTOs(officeHours));
    }
}
