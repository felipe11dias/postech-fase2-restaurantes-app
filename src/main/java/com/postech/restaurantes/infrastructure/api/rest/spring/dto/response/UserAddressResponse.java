package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.UserAddressView;
import java.util.UUID;

/** Endereço do usuário no corpo da resposta: o vínculo (id, rótulo, padrão) e o endereço aninhado. */
public record UserAddressResponse(UUID id, String label, boolean isDefault, AddressResponse address) {

    public static UserAddressResponse from(UserAddressView view) {
        return new UserAddressResponse(view.id(), view.label(), view.isDefault(), AddressResponse.from(view.address()));
    }
}
