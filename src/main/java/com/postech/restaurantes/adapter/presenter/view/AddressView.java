package com.postech.restaurantes.adapter.presenter.view;

import java.util.Objects;
import java.util.UUID;

public record AddressView(UUID id, CityView city, String street, String number, String complement,
                          String neighborhood, String zipCode) {
    public AddressView {
        Objects.requireNonNull(city, "Cidade do endereço inválida");
    }
}
