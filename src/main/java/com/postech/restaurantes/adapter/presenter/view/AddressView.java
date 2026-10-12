package com.postech.restaurantes.adapter.presenter.view;

import java.util.UUID;

public record AddressView(UUID id, UUID cityId, String street, String number, String complement, String neighborhood,
                          String city, String state, String zipCode) {

    public AddressView(UUID id, String street, String number, String complement, String neighborhood,
                       String city, String state, String zipCode) {
        this(id, null, street, number, complement, neighborhood, city, state, zipCode);
    }
}
