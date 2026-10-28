package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.cuisine.UpdateCuisineDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateCuisineRequest(
        @Schema(description = "Novo nome do tipo de cozinha; único no catálogo, sem diferenciar maiúsculas",
                example = "Italiana")
        @NotBlank @Size(max = 100) String name
) {

    public UpdateCuisineDTO toDTO(UUID id) {
        return new UpdateCuisineDTO(id, name);
    }
}