package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_REQUEST;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.controller.RestaurantController;
import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.RestaurantModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CreateRestaurantRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.AuthenticatedUser;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateRestaurantRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.RestaurantResponse;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RestaurantRestControllerTest {

    private RestaurantController controller;
    private RestaurantModelAssembler assembler;
    private RestaurantRestController restController;

    private UUID restaurantId;
    private UUID userId;
    private RestaurantView view;

    @BeforeEach
    void setUp() {
        controller = mock(RestaurantController.class);
        assembler = new RestaurantModelAssembler();
        restController = new RestaurantRestController(controller, assembler);

        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        restaurantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        view = new RestaurantView(restaurantId, userId, ADDRESS_VIEW, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Create endpoint devolve HTTP 201 com Location")
    void deveCriar() {
        when(controller.create(any())).thenReturn(view);

        CreateRestaurantRequest req = new CreateRestaurantRequest(userId, ADDRESS_REQUEST, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        ResponseEntity<EntityModel<RestaurantResponse>> response =
                restController.create(req, new AuthenticatedUser(userId, "dono", Set.of("ROLE_OWNER")));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
    }

    @Test
    @DisplayName("FindById endpoint devolve EntityModel")
    void deveBuscarPorId() {
        when(controller.findById(restaurantId)).thenReturn(view);

        EntityModel<RestaurantResponse> response = restController.findById(restaurantId);

        assertNotNull(response.getContent());
    }

    @Test
    @DisplayName("Search endpoint devolve PagedModel")
    void deveBuscarPaginado() {
        PageResult<RestaurantView> page = new PageResult<>(List.of(view), 0, 10, 1);
        UUID dono = UUID.randomUUID();
        when(controller.search(eq("sabor"), eq(dono), any())).thenReturn(page);

        PagedModel<EntityModel<RestaurantResponse>> response = restController.search("sabor", dono, 0, 10, "name,asc");

        assertNotNull(response);
    }

    @Test
    @DisplayName("Update endpoint devolve EntityModel")
    void deveAtualizar() {
        when(controller.update(any())).thenReturn(view);

        UpdateRestaurantRequest req = new UpdateRestaurantRequest(userId, ADDRESS_REQUEST, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        EntityModel<RestaurantResponse> response = restController.update(restaurantId, req);

        assertNotNull(response.getContent());
    }

    @Test
    @DisplayName("Delete endpoint chama controller")
    void deveDeletar() {
        restController.delete(restaurantId);

        verify(controller).delete(restaurantId);
    }

    @Test
    @DisplayName("Sem o parâmetro de ordenação, a página vai sem ordenação pedida")
    void deveAceitarBuscaSemOrdenacao() {
        assertNull(RestaurantRestController.paginacao(0, 20, null).sortBy());
        assertNull(RestaurantRestController.paginacao(0, 20, "   ").sortBy());
    }

    @Test
    @DisplayName("Ordenação sem direção explícita é crescente")
    void deveAssumirOrdemCrescente() {
        PageRequest pedido = RestaurantRestController.paginacao(0, 20, "name");

        assertEquals("name", pedido.sortBy());
        assertEquals(SortDirection.ASC, pedido.direction());
    }

    @Test
    @DisplayName("A direção informada é respeitada")
    void deveLerADirecao() {
        assertEquals(SortDirection.DESC, RestaurantRestController.paginacao(0, 20, "name,desc").direction());
    }
}
