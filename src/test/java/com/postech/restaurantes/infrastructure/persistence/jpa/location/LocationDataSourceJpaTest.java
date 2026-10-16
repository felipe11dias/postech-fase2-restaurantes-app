package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

class LocationDataSourceJpaTest {
    @Test
    @DisplayName("Reads seeded states without writing to the catalog")
    void shouldReadStates() {
        var states = mock(SpringDataStateRepository.class);
        var cities = mock(SpringDataCityRepository.class);
        when(states.findAllByOrderByNameAsc()).thenReturn(List.of(PersistenceFixtures.stateEntity()));
        assertEquals(List.of(LocationFixtures.STATE_DATA), new LocationDataSourceJpa(states, cities).findStates());
        verifyNoInteractions(cities);
    }

    @Test
    @DisplayName("Queries cities with a stable order and optional filters")
    void shouldSearchCities() {
        var states = mock(SpringDataStateRepository.class);
        var cities = mock(SpringDataCityRepository.class);
        var source = new LocationDataSourceJpa(states, cities);
        when(cities.search(eq(LocationFixtures.STATE_ID), eq("São"), any()))
                .thenReturn(new PageImpl<>(List.of(PersistenceFixtures.cityEntity())));
        var result = source.searchCities(LocationFixtures.STATE_ID, "São", PageRequest.of(0, 5));
        assertEquals(LocationFixtures.CITY_DATA, result.content().getFirst());
        assertEquals(1, result.totalElements());
        assertEquals(5, result.size());
        verify(cities).search(eq(LocationFixtures.STATE_ID), eq("São"), argThat(page ->
                page.getPageSize() == 5 && page.getSort().getOrderFor("id") != null));
        when(cities.search(isNull(), eq(""), any())).thenReturn(org.springframework.data.domain.Page.empty());
        assertTrue(source.searchCities(null, null, PageRequest.first()).content().isEmpty());
    }

    @Test
    @DisplayName("Resolves only existing city references and rejects missing or unknown ids")
    void shouldResolveCityReferences() {
        var cities = mock(SpringDataCityRepository.class);
        var resolver = new CityReferenceResolver(cities);
        var city = PersistenceFixtures.cityEntity();
        when(cities.findById(LocationFixtures.CITY_ID)).thenReturn(Optional.of(city));
        assertSame(city, resolver.resolve(LocationFixtures.CITY_ID));
        assertThrows(com.postech.restaurantes.domain.exception.InvariantViolationException.class,
                () -> resolver.resolve(null));
        UUID unknown = UUID.randomUUID();
        assertThrows(com.postech.restaurantes.domain.exception.ResourceNotFoundException.class,
                () -> resolver.resolve(unknown));
    }
}
