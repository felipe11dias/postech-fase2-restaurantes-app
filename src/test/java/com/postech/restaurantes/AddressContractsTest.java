package com.postech.restaurantes;

import static org.junit.jupiter.api.Assertions.*;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.presenter.view.AddressView;
import com.postech.restaurantes.adapter.presenter.view.CityView;
import com.postech.restaurantes.adapter.presenter.view.StateView;
import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.AddressRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressContractsTest {
    @Test
    @DisplayName("Every address boundary rejects a missing city at construction")
    void shouldRejectMissingCityAcrossBoundaries() {
        assertThrows(IllegalArgumentException.class, () -> new AddressDTO(null, "Rua A", "1", null, "Centro", "01001000"));
        assertThrows(IllegalArgumentException.class, () -> new AddressRequest(null, "Rua A", "1", null, "Centro", "01001000"));
        assertThrows(NullPointerException.class, () -> new AddressData(null, null, "Rua A", "1", null, "Centro", "01001000"));
        assertThrows(NullPointerException.class, () -> new AddressView(null, null, "Rua A", "1", null, "Centro", "01001000"));
        assertThrows(NullPointerException.class, () -> new CityView(null, "Cidade", new StateView(LocationFixtures.STATE_ID, "São Paulo", "SP")));
        assertThrows(NullPointerException.class, () -> new CityView(LocationFixtures.CITY_ID, "Cidade", null));
        assertThrows(NullPointerException.class, () -> new StateView(null, "São Paulo", "SP"));
    }
}
