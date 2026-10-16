package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.postech.restaurantes.domain.exception.InvariantViolationException;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Endereço como chega pelo HTTP: referência obrigatória a uma cidade do catálogo. */
public record AddressRequest(
        @Schema(description = "Id de uma cidade já cadastrada", example = "c0000000-0000-4000-8000-000000000001")
        @NotNull UUID cityId,
        @Schema(example = "Rua das Flores") @NotBlank @Size(max = 150) String street,
        @Schema(example = "100") @NotBlank @Size(max = 20) String number,
        @Schema(example = "Apto 21") @Size(max = 100) String complement,
        @Schema(example = "Centro") @NotBlank @Size(max = 100) String neighborhood,
        @Schema(description = "8 dígitos, com ou sem máscara", example = "01001-000")
        @NotBlank @Size(max = 9) String zipCode) {
    public AddressRequest {
        if (cityId == null) {
            throw new InvariantViolationException("Id da cidade do endereço inválido");
        }
    }

    public AddressDTO toDTO() {
        return new AddressDTO(cityId, street, number, complement, neighborhood, zipCode);
    }
}
