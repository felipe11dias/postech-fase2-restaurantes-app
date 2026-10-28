package com.postech.restaurantes.adapter.datasource.data;

import java.util.Objects;
import java.util.UUID;

/** A escrita usa cityId; a leitura inclui os dados do catálogo para os presenters. */
public record AddressData(UUID id, UUID cityId, String street, String number, String complement,
                          String neighborhood, String zipCode, CityData city) {
    public AddressData {
        Objects.requireNonNull(cityId, "Id da cidade do endereço inválido");
    }

    public AddressData(UUID id, UUID cityId, String street, String number, String complement,
                       String neighborhood, String zipCode) {
        this(id, cityId, street, number, complement, neighborhood, zipCode, null);
    }
}
