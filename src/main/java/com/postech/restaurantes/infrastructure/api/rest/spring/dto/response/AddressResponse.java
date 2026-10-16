package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import java.util.UUID;

/** Endereço com cidade e estado do catálogo; o CEP sai sem máscara. */
public record AddressResponse(UUID id, CityResponse city, String street, String number, String complement,
                              String neighborhood, String zipCode) {
    public static AddressResponse from(AddressView view) {
        return new AddressResponse(view.id(), CityResponse.from(view.city()), view.street(), view.number(),
                view.complement(), view.neighborhood(), view.zipCode());
    }
}
