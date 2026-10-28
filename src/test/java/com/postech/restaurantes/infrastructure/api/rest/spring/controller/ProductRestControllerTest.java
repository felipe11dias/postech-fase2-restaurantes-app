package com.postech.restaurantes.infrastructure.api.rest.spring.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.restaurantes.adapter.controller.ProductController;
import com.postech.restaurantes.adapter.presenter.view.ProductView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.assembler.ProductModelAssembler;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ProductRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.ProductResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ProductRestControllerTest {

    private ProductController controller;
    private ProductRestController restController;
    private UUID restaurantId;
    private UUID productId;
    private ProductView view;
    private ProductRequest request;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @BeforeEach
    void setUp() {
        controller = mock(ProductController.class);
        restController = new ProductRestController(controller, new ProductModelAssembler());
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest()));
        restaurantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        view = new ProductView(productId, restaurantId, "Pizza", null, new BigDecimal("20.00"),
                false, List.of("/images/pizza.jpg"), List.of(
                new ProductView.ProductOptionGroupView("Tamanho", false, 0, 1,
                        List.of(new ProductView.ProductOptionValueView("Grande", BigDecimal.ZERO)))));
        request = new ProductRequest("Pizza", null, new BigDecimal("20.00"), false,
                List.of("/images/pizza.jpg"),
                List.of(new ProductRequest.ProductOptionGroupRequest("Tamanho", false, 0, 1,
                        List.of(new ProductRequest.ProductOptionValueRequest("Grande", BigDecimal.ZERO)))));
    }

    @Test
    @DisplayName("Cria produto com resposta 201 e Location")
    void deveCriarProduto() throws Exception {
        when(controller.create(eq(restaurantId), any())).thenReturn(view);

        var response = restController.create(restaurantId, request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getHeaders().getLocation());
        assertEquals("Grande", response.getBody().getContent().optionGroups().getFirst().values().getFirst().name());
        String json = new ObjectMapper().writeValueAsString(response.getBody().getContent());
        assertEquals("/images/pizza.jpg", new ObjectMapper().readTree(json).get("imagePaths").get(0).asText());
    }

    @Test
    @DisplayName("Lista produtos com paginação")
    void deveListarProdutos() {
        when(controller.search(eq(restaurantId), any()))
                .thenReturn(new PageResult<>(List.of(view), 0, 10, 1));

        PagedModel<EntityModel<ProductResponse>> page = restController.search(restaurantId, 0, 10);

        assertEquals(1, page.getContent().size());
        verify(controller).search(eq(restaurantId), eq(PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Consulta e atualiza produto")
    void deveConsultarEAtualizar() {
        when(controller.findById(restaurantId, productId)).thenReturn(view);
        when(controller.update(eq(restaurantId), eq(productId), any())).thenReturn(view);

        assertNotNull(restController.findById(restaurantId, productId).getContent());
        assertNotNull(restController.update(restaurantId, productId, request).getContent());
    }

    @Test
    @DisplayName("Exclui produto")
    void deveExcluirProduto() {
        restController.delete(restaurantId, productId);
        verify(controller).delete(restaurantId, productId);
    }
}
