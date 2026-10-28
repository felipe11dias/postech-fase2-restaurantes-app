package com.postech.restaurantes.adapter.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.adapter.AdapterFixtures.CountingUnitOfWork;
import com.postech.restaurantes.adapter.datasource.ILocationDataSource;
import com.postech.restaurantes.adapter.gateway.LocationGateway;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LocationControllerTest {
    @Test
    @DisplayName("Rejects missing dependencies")
    void shouldRejectMissingDependencies() {
        var source = mock(ILocationDataSource.class);
        assertThrows(IllegalArgumentException.class, () -> LocationController.create(null, new CountingUnitOfWork()));
        assertThrows(IllegalArgumentException.class, () -> LocationController.create(source, null));
        assertThrows(IllegalArgumentException.class, () -> LocationGateway.create(null));
    }

    @Test
    @DisplayName("Lists catalog states through a use case and a unit of work")
    void shouldListStates() {
        var source = mock(ILocationDataSource.class);
        when(source.findStates()).thenReturn(List.of(LocationFixtures.STATE_DATA));
        var unitOfWork = new CountingUnitOfWork();
        var states = LocationController.create(source, unitOfWork).findStates();
        assertEquals("São Paulo", states.getFirst().name());
        assertEquals("SP", states.getFirst().acronym());
        assertEquals(LocationFixtures.STATE_ID, states.getFirst().id());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Searches cities with trimmed names, fixed sorting and preserved pagination")
    void shouldSearchCities() {
        var source = mock(ILocationDataSource.class);
        var request = PageRequest.of(2, 5).withSort("state", SortDirection.DESC);
        var expected = PageRequest.of(2, 5).withSort("name", SortDirection.ASC);
        when(source.searchCities(LocationFixtures.STATE_ID, "São", expected))
                .thenReturn(new PageResult<>(List.of(LocationFixtures.CITY_DATA), 2, 5, 11));
        var unitOfWork = new CountingUnitOfWork();
        var cities = LocationController.create(source, unitOfWork).searchCities(LocationFixtures.STATE_ID, " São ", request);
        assertEquals("São Paulo", cities.content().getFirst().name());
        assertEquals("SP", cities.content().getFirst().state().acronym());
        assertEquals(LocationFixtures.CITY_ID, cities.content().getFirst().id());
        assertEquals(11, cities.totalElements());
        assertEquals(2, cities.page());
        assertEquals(1, unitOfWork.executions());
    }

    @Test
    @DisplayName("Rejects missing pagination before querying cities")
    void shouldRejectMissingPagination() {
        var source = mock(ILocationDataSource.class);
        var controller = LocationController.create(source, new CountingUnitOfWork());
        assertThrows(IllegalArgumentException.class, () -> controller.searchCities(null, null, null));
        verifyNoInteractions(source);
    }
}
