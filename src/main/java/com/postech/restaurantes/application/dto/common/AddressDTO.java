package com.postech.restaurantes.application.dto.common;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import java.util.UUID;

/** Entrada de endereço: a cidade é escolhida no catálogo, nunca criada pelo cadastro. */
public record AddressDTO(UUID cityId, String street, String number, String complement, String neighborhood,
                         String zipCode) {
    public AddressDTO {
        Guard.requireNonNull(cityId, "Id da cidade do endereço inválido");
    }

    public Address toEntity() {
        return Address.create(cityId, street, number, complement, neighborhood, zipCode);
    }
}
