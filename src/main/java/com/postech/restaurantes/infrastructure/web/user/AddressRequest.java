package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Endereço como chega pelo HTTP. A validação aqui é <strong>sintática</strong> — campo
 * presente, tamanho aceitável. A consistência ("CEP tem 8 dígitos") continua sendo do objeto
 * de valor do domínio: a borda recusa cedo o que é obviamente inválido, sem tirar do domínio
 * a palavra final.
 */
public record AddressRequest(
        @Schema(example = "Rua das Flores") @NotBlank @Size(max = 150) String street,
        @Schema(example = "100") @NotBlank @Size(max = 20) String number,
        @Schema(example = "Apto 21") @Size(max = 100) String complement,
        @Schema(example = "Centro") @NotBlank @Size(max = 100) String neighborhood,
        @Schema(example = "São Paulo") @NotBlank @Size(max = 100) String city,
        @Schema(description = "UF", example = "SP") @NotBlank @Size(min = 2, max = 2) String state,
        @Schema(description = "8 dígitos, com ou sem máscara; a resposta devolve sem máscara", example = "01001-000")
        @NotBlank @Size(max = 9) String zipCode) {

    public AddressDTO toDTO() {
        return new AddressDTO(street, number, complement, neighborhood, city, state, zipCode);
    }

    public static List<AddressDTO> toDTOs(List<AddressRequest> requests) {
        return requests == null ? null : requests.stream().map(AddressRequest::toDTO).toList();
    }
}
