package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import com.postech.restaurantes.domain.entity.address.Address;

/** Saída do endereço, a mesma para o usuário e para o restaurante. O CEP sai normalizado. */
public final class AddressPresenter {

    private AddressPresenter() {
    }

    public static AddressView toView(Address address) {
        return new AddressView(address.getId(), LocationPresenter.toView(address.getCity()), address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getZipCode().value());
    }
}
