package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import com.postech.restaurantes.domain.entity.address.Address;
import java.util.Objects;

/** Saída do endereço, a mesma para o usuário e para o restaurante. O CEP sai normalizado. */
public final class AddressPresenter {

    private AddressPresenter() {
    }

    public static AddressView toView(Address address) {
        var city = Objects.requireNonNull(address.getCity(), "Cidade do endereço inválida");
        return new AddressView(address.getId(), LocationPresenter.toView(city),
                address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getZipCode().value());
    }
}
