package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.controller.CuisineController;
import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.cuisine.CreateCuisineDTO;
import com.postech.restaurantes.application.dto.cuisine.UpdateCuisineDTO;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.CuisineModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CreateCuisineRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateCuisineRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.CuisineResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class CuisineRestControllerTest {

    private CuisineController controller;
    private CuisineRestController restController;
    private UUID id;
    private CuisineView view;

    @BeforeEach
    void setUp() {
        controller = mock(CuisineController.class);
        restController = new CuisineRestController(controller, new CuisineModelAssembler());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        id = UUID.randomUUID();
        view = new CuisineView(id, "Italiana", null, null);
    }

    @Test
    @DisplayName("Criar entrega o nome ao núcleo e responde 201 com Location")
    void deveCriar() {
        when(controller.create(any())).thenReturn(view);

        ResponseEntity<EntityModel<CuisineResponse>> resposta = restController.create(new CreateCuisineRequest("Italiana"));

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertTrue(resposta.getHeaders().getLocation().toString().endsWith("/api/v1/cuisines/" + id));
        assertEquals("Italiana", resposta.getBody().getContent().name());
        verify(controller).create(new CreateCuisineDTO("Italiana"));
    }

    @Test
    @DisplayName("Buscar por id devolve o tipo com links")
    void deveBuscarPorId() {
        when(controller.findById(id)).thenReturn(view);

        EntityModel<CuisineResponse> resposta = restController.findById(id);

        assertEquals("Italiana", resposta.getContent().name());
        assertTrue(resposta.hasLink("self"));
    }

    @Test
    @DisplayName("Listar converte os parâmetros da URL no pedido de página")
    void deveListar() {
        when(controller.search(eq("ital"), any())).thenReturn(new PageResult<>(List.of(view), 0, 10, 1));

        PagedModel<EntityModel<CuisineResponse>> resposta = restController.search("ital", 0, 10, "name,desc");

        assertEquals(1, resposta.getContent().size());
        ArgumentCaptor<PageRequest> pedido = ArgumentCaptor.forClass(PageRequest.class);
        verify(controller).search(eq("ital"), pedido.capture());
        assertEquals("name", pedido.getValue().sortBy());
        assertEquals(10, pedido.getValue().size());
    }

    @Test
    @DisplayName("Alterar junta o id da URL com o nome do corpo")
    void deveAtualizar() {
        when(controller.update(any())).thenReturn(view);

        EntityModel<CuisineResponse> resposta = restController.update(id, new UpdateCuisineRequest("Italiana"));

        assertEquals("Italiana", resposta.getContent().name());
        verify(controller).update(new UpdateCuisineDTO(id, "Italiana"));
    }

    @Test
    @DisplayName("Excluir repassa o id ao núcleo")
    void deveExcluir() {
        restController.delete(id);

        verify(controller).delete(id);
    }
}
