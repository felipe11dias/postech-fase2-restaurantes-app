package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.RestaurantResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RestaurantModelAssemblerTest {

    private RestaurantModelAssembler assembler;
    private RestaurantView view;

    @BeforeEach
    void setUp() {
        assembler = new RestaurantModelAssembler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        view = new RestaurantView(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Sabor",
                LocalTime.of(8, 0), LocalTime.of(22, 0), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Converte view para EntityModel com HATEOAS links")
    void deveConverterParaModel() {
        EntityModel<RestaurantResponse> model = assembler.toModel(view);

        assertNotNull(model.getContent());
        assertEquals("Sabor", model.getContent().name());
        assertTrue(model.hasLink("self"));
    }

    @Test
    @DisplayName("Converte pagina de views para PagedModel com navegacao")
    void deveConverterParaPagedModel() {
        PageResult<RestaurantView> page = new PageResult<>(List.of(view), 1, 10, 25);

        PagedModel<EntityModel<RestaurantResponse>> pagedModel = assembler.toPagedModel(page, "sabor", "name,asc");

        assertNotNull(pagedModel.getContent());
        assertTrue(pagedModel.hasLink("self"));
        assertTrue(pagedModel.hasLink("first"));
        assertTrue(pagedModel.hasLink("prev"));
        assertTrue(pagedModel.hasLink("next"));
        assertTrue(pagedModel.hasLink("last"));
    }
}
