package com.postech.restaurantes.adapter.datasource.data;

import java.util.UUID;

/** Endereço como a origem de dados o conhece, com cidade por id e CEP sem máscara. */
public record AddressData(UUID id, UUID cityId, String street, String number, String complement, String neighborhood,
                          String city, String state, String zipCode) {

    public AddressData(UUID id, String street, String number, String complement, String neighborhood,
                       String city, String state, String zipCode) {
        this(id, null, street, number, complement, neighborhood, city, state, zipCode);
    }
}
