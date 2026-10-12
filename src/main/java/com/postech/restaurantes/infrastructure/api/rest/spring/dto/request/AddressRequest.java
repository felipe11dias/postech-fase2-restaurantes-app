package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Endereço como chega pelo HTTP. Cidade e UF podem ser omitidas quando {@code cityId} é informado;
 * sem ele, o domínio exige os dois nomes para resolver a localização existente.
 */
public record AddressRequest(
        @Schema(description = "Id da cidade; quando city ou UF também forem enviados, devem corresponder",
                example = "c0000000-0000-4000-8000-000000000001")
        UUID cityId,
        @Schema(example = "Rua das Flores") @NotBlank @Size(max = 150) String street,
        @Schema(example = "100") @NotBlank @Size(max = 20) String number,
        @Schema(example = "Apto 21") @Size(max = 100) String complement,
        @Schema(example = "Centro") @NotBlank @Size(max = 100) String neighborhood,
        @Schema(description = "Cidade; opcional quando cityId é informado", example = "São Paulo")
        @Size(max = 100) String city,
        @Schema(description = "UF; opcional quando cityId é informado", example = "SP")
        @Size(min = 2, max = 2) String state,
        @Schema(description = "8 dígitos, com ou sem máscara; a resposta devolve sem máscara", example = "01001-000")
        @NotBlank @Size(max = 9) String zipCode) {

    public AddressRequest(String street, String number, String complement, String neighborhood,
                          String city, String state, String zipCode) {
        this(null, street, number, complement, neighborhood, city, state, zipCode);
    }

    public AddressDTO toDTO() {
        return new AddressDTO(cityId, street, number, complement, neighborhood, city, state, zipCode);
    }

    public static List<AddressDTO> toDTOs(List<AddressRequest> requests) {
        return requests == null ? null : requests.stream().map(AddressRequest::toDTO).toList();
    }
}
