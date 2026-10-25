package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.CuisineResponse;
import java.time.LocalDateTime;
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

class CuisineModelAssemblerTest {

    private CuisineModelAssembler assembler;
    private CuisineView view;

    @BeforeEach
    void setUp() {
        assembler = new CuisineModelAssembler();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        view = new CuisineView(UUID.randomUUID(), "Italiana", LocalDateTime.of(2026, 1, 1, 10, 0), null);
    }

    @Test
    @DisplayName("Um tipo vira resposta com os dados e os links self e cuisines")
    void deveConverterParaModel() {
        EntityModel<CuisineResponse> model = assembler.toModel(view);

        assertEquals(new CuisineResponse(view.id(), "Italiana", view.createdAt(), null), model.getContent());
        assertTrue(model.getRequiredLink("self").getHref().endsWith("/api/v1/cuisines/" + view.id()));
        assertTrue(model.getRequiredLink("cuisines").getHref().endsWith("/api/v1/cuisines"));
    }

    @Test
    @DisplayName("Página do meio tem links de navegação que repetem a busca e a ordenação")
    void deveConverterPaginaComNavegacao() {
        PageResult<CuisineView> pagina = new PageResult<>(List.of(view), 1, 10, 25);

        PagedModel<EntityModel<CuisineResponse>> model = assembler.toPagedModel(pagina, "ital", "name,asc");

        assertEquals(1, model.getContent().size());
        assertTrue(model.hasLink("self"));
        assertTrue(model.hasLink("first"));
        assertTrue(model.hasLink("prev"));
        assertTrue(model.hasLink("last"));
        String proxima = model.getRequiredLink("next").getHref();
        assertTrue(proxima.contains("page=2"));
        assertTrue(proxima.contains("name=ital"));
        assertTrue(proxima.contains("sort=name,asc"));
    }

    @Test
    @DisplayName("Página única não tem anterior nem próxima, e sem busca os parâmetros não aparecem")
    void deveOmitirLinksQueNaoExistem() {
        PagedModel<EntityModel<CuisineResponse>> model =
                assembler.toPagedModel(new PageResult<>(List.of(), 0, 10, 0), null, null);

        assertFalse(model.hasLink("prev"));
        assertFalse(model.hasLink("next"));
        assertTrue(model.getRequiredLink("last").getHref().contains("page=0"));
        assertFalse(model.getRequiredLink("self").getHref().contains("name="));
        assertFalse(model.getRequiredLink("self").getHref().contains("sort="));
    }
}
