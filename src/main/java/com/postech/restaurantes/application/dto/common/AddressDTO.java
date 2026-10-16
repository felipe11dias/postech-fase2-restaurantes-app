package com.postech.restaurantes.application.dto.common;

import com.postech.restaurantes.domain.entity.address.Address;
import java.util.UUID;

/** Dados de endereço recebidos pelos casos de uso. A conversão para o domínio vive aqui. */
public record AddressDTO(UUID cityId, String street, String number, String complement, String neighborhood,
                         String city, String state, String zipCode) {

    public AddressDTO(String street, String number, String complement, String neighborhood,
                      String city, String state, String zipCode) {
        this(null, street, number, complement, neighborhood, city, state, zipCode);
    }

    public Address toEntity() {
        return Address.create(street, number, complement, neighborhood, city, state, zipCode);
    }
}
