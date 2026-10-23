package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.adapter.controller.LocationController;
import com.postech.restaurantes.adapter.presenter.LocationPresenter;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.LocationModelAssembler;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class LocationRestControllerTest {
    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() { RequestContextHolder.resetRequestAttributes(); }

    @Test
    @DisplayName("Returns catalog states and cities as HTTP representations")
    void shouldReturnCatalogRepresentations() {
        var controller = mock(LocationController.class);
        var assembler = new LocationModelAssembler();
        var rest = new LocationRestController(controller, assembler);
        var city = LocationPresenter.toView(LocationFixtures.city("São Paulo", "SP"));
        when(controller.findStates()).thenReturn(List.of(city.state()));
        assertEquals("SP", rest.findStates().getContent().iterator().next().acronym());
        when(controller.searchCities(LocationFixtures.STATE_ID, "São", PageRequest.of(0, 5)))
                .thenReturn(new PageResult<>(List.of(city), 0, 5, 1));
        var page = rest.searchCities(LocationFixtures.STATE_ID, "São", 0, 5);
        assertEquals("São Paulo", page.getContent().iterator().next().name());
        assertEquals(LocationFixtures.STATE_ID, page.getContent().iterator().next().state().id());
        assertTrue(page.hasLink("self"));
        assertTrue(page.hasLink("first"));
        assertTrue(page.hasLink("last"));
        assertFalse(page.hasLink("prev"));
        assertFalse(page.hasLink("next"));
    }

    @Test
    @DisplayName("Preserves encoded city filters in pagination links")
    void shouldPreserveFiltersInLinks() {
        var assembler = new LocationModelAssembler();
        var city = LocationFixtures.cityView("São Paulo", "SP");
        var result = assembler.toPagedModel(new PageResult<>(List.of(city), 1, 1, 3), LocationFixtures.STATE_ID, "São & A+B");
        String next = result.getRequiredLink("next").getHref();
        assertTrue(next.contains("stateId=" + LocationFixtures.STATE_ID));
        assertTrue(next.contains("name=S%C3%A3o%20%26%20A+B"));
        assertTrue(next.contains("page=2"));
        assertTrue(result.hasLink("prev"));
        var empty = assembler.toPagedModel(new PageResult<>(List.of(), 0, 5, 0), null, null);
        assertFalse(empty.getRequiredLink("self").getHref().contains("stateId="));
        assertFalse(empty.getRequiredLink("self").getHref().contains("name="));
        assertTrue(empty.getRequiredLink("last").getHref().contains("page=0"));
    }
}
