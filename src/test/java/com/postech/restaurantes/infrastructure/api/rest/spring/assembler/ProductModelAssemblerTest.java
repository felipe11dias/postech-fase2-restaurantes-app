package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.adapter.presenter.view.ProductView;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class ProductModelAssemblerTest {

    private final ProductModelAssembler assembler = new ProductModelAssembler();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private final ProductView view = new ProductView(productId, restaurantId, "Pizza", null, new BigDecimal("10.00"),
            false, List.of("/images/pizza.jpg"), List.of());

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Monta links do produto sob o restaurante")
    void deveMontarModeloDeProduto() {
        setRequestContext();

        var model = assembler.toModel(view);

        assertEquals(productId, model.getContent().id());
        assertTrue(model.getRequiredLink(IanaLinkRelations.SELF).getHref().contains(restaurantId.toString()));
    }

    @Test
    @DisplayName("Monta links de navegação da página")
    void deveMontarModeloPaginado() {
        setRequestContext();
        PageResult<ProductView> page = new PageResult<>(List.of(view), 1, 1, 3);

        var model = assembler.toPagedModel(page, restaurantId);

        assertTrue(model.hasLink(IanaLinkRelations.PREV));
        assertTrue(model.hasLink(IanaLinkRelations.NEXT));
        assertTrue(model.hasLink(IanaLinkRelations.FIRST));
        assertTrue(model.hasLink(IanaLinkRelations.LAST));
    }

    @Test
    @DisplayName("Página única não tem links anterior ou próximo")
    void deveMontarPaginaUnica() {
        setRequestContext();
        PageResult<ProductView> page = new PageResult<>(List.of(view), 0, 1, 1);

        var model = assembler.toPagedModel(page, restaurantId);

        assertTrue(!model.hasLink(IanaLinkRelations.PREV));
        assertTrue(!model.hasLink(IanaLinkRelations.NEXT));
    }

    private static void setRequestContext() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }
}
