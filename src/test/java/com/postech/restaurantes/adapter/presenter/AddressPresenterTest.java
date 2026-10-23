package com.postech.restaurantes.adapter.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.domain.entity.address.Address;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressPresenterTest {

    @Test
    @DisplayName("O presenter recusa endereço sem cidade carregada antes de produzir a view")
    void shouldRejectAddressWithoutLoadedCity() {
        var address = Address.create(LocationFixtures.CITY_ID, "Rua A", "1", null, "Centro", "01001000");

        var exception = assertThrows(NullPointerException.class, () -> AddressPresenter.toView(address));

        assertEquals("Cidade do endereço inválida", exception.getMessage());
    }
}
