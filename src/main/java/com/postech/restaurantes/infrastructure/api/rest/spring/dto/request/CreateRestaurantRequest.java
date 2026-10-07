package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Corpo da requisição para criação de restaurante. Sem {@code userId}, o dono é quem está autenticado; só
 * um administrador indica outro dono (regra de posse, no {@code @PreAuthorize}).
 */
public record CreateRestaurantRequest(
        @Schema(description = "Dono do restaurante. Opcional: ausente, é quem está autenticado; outro, só administrador")
        UUID userId,
        @Schema(description = "Endereço do próprio restaurante (não é escolhido entre os do dono)")
        @NotNull @Valid AddressRequest address,
        @Schema(example = "Restaurante Sabor & Arte") @NotBlank @Size(max = 150) String name,
        @Schema(description = "Horários de funcionamento por dia da semana; ao menos um, sem sobreposição")
        @NotNull @Valid List<OfficeHourRequest> officeHours
) {

    /** {@code authenticatedUserId} é o dono quando o corpo não indica nenhum. */
    public CreateRestaurantDTO toDTO(UUID authenticatedUserId) {
        return new CreateRestaurantDTO(userId != null ? userId : authenticatedUserId,
                address == null ? null : address.toDTO(), name,
                OfficeHourRequest.toDTOs(officeHours));
    }
}
