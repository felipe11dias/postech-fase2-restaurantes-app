package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.application.dto.common.AddressDTO;
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
        @NotBlank @Size(max = 150) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @NotBlank @Size(max = 100) String neighborhood,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(min = 2, max = 2) String state,
        @NotBlank @Size(max = 9) String zipCode) {

    public AddressDTO toDTO() {
        return new AddressDTO(street, number, complement, neighborhood, city, state, zipCode);
    }

    public static List<AddressDTO> toDTOs(List<AddressRequest> requests) {
        return requests == null ? null : requests.stream().map(AddressRequest::toDTO).toList();
    }
}
