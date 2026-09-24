package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import java.util.UUID;

/** Endereço no corpo da resposta HTTP. O CEP sai normalizado, como o domínio o guarda. */
public record AddressResponse(UUID id, String street, String number, String complement, String neighborhood,
                              String city, String state, String zipCode) {

    public static AddressResponse from(AddressView view) {
        return new AddressResponse(view.id(), view.street(), view.number(), view.complement(),
                view.neighborhood(), view.city(), view.state(), view.zipCode());
    }
}
