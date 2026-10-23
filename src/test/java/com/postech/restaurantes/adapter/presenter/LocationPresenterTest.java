package com.postech.restaurantes.adapter.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.domain.entity.location.city.City;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocationPresenterTest {

    @Test
    @DisplayName("O presenter recusa cidade sem id antes de produzir a view")
    void shouldRejectCityWithoutId() {
        var state = LocationFixtures.city("São Paulo", "SP").getState();
        var city = City.create(state, "Cidade");

        var exception = assertThrows(NullPointerException.class, () -> LocationPresenter.toView(city));

        assertEquals("Id inválido", exception.getMessage());
    }

    @Test
    @DisplayName("O presenter recusa cidade sem estado antes de produzir a view")
    void shouldRejectCityWithoutState() {
        var city = mock(City.class);
        when(city.getId()).thenReturn(LocationFixtures.CITY_ID);

        var exception = assertThrows(NullPointerException.class, () -> LocationPresenter.toView(city));

        assertEquals("Estado da cidade inválido", exception.getMessage());
    }
}
