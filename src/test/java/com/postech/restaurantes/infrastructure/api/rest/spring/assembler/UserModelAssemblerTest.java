package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.UserResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Links de navegação HATEOAS: montados a partir da requisição atual e das rotas da API. */
class UserModelAssemblerTest {

    private final UserModelAssembler assembler = new UserModelAssembler();

    @BeforeEach
    void abrirRequisicao() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void fecharRequisicao() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Um usuário ganha o link para si mesmo e para a coleção")
    void deveMontarOsLinksDoUsuario() {
        EntityModel<UserResponse> model = assembler.toModel(USER_VIEW);

        assertEquals("http://localhost" + ApiRoutes.USERS + "/" + USER_ID,
                model.getLink(IanaLinkRelations.SELF).orElseThrow().getHref());
        assertEquals("http://localhost" + ApiRoutes.USERS,
                model.getLink(UserModelAssembler.USERS_REL).orElseThrow().getHref());
        assertEquals(USER_ID, model.getContent().id());
    }

    @Test
    @DisplayName("A página preserva os metadados do núcleo e dá link a cada item")
    void deveMontarAPagina() {
        PagedModel<EntityModel<UserResponse>> pagina =
                assembler.toPagedModel(new PageResult<>(List.of(USER_VIEW), 2, 5, 11), null, null);

        assertEquals(1, pagina.getContent().size());
        assertEquals(5, pagina.getMetadata().getSize());
        assertEquals(2, pagina.getMetadata().getNumber());
        assertEquals(11, pagina.getMetadata().getTotalElements());
        assertEquals(3, pagina.getMetadata().getTotalPages());
        assertTrue(pagina.getContent().iterator().next().getLink(IanaLinkRelations.SELF).isPresent());
    }

    @Test
    @DisplayName("Página do meio tem os cinco links, e cada um repete a busca e a ordenação pedidas")
    void deveMontarOsLinksDePaginacaoQuandoHaPaginasAntesEDepois() {
        PagedModel<EntityModel<UserResponse>> pagina = assembler.toPagedModel(
                new PageResult<>(List.of(USER_VIEW), 1, 5, 11), "Ana", "name,desc");

        assertTrue(href(pagina, IanaLinkRelations.SELF).endsWith("/users?name=Ana&page=1&size=5&sort=name,desc"));
        assertTrue(href(pagina, IanaLinkRelations.FIRST).contains("page=0&"));
        assertTrue(href(pagina, IanaLinkRelations.PREV).contains("page=0&"));
        assertTrue(href(pagina, IanaLinkRelations.NEXT).contains("page=2&"));
        assertTrue(href(pagina, IanaLinkRelations.LAST).contains("page=2&"));
        assertTrue(pagina.getLink(UserModelAssembler.USERS_REL).isPresent());
    }

    @Test
    @DisplayName("Página única não tem anterior nem próxima; primeira e última são ela mesma")
    void naoDeveOferecerAnteriorNemProximaQuandoHaUmaSoPagina() {
        PagedModel<EntityModel<UserResponse>> pagina = assembler.toPagedModel(
                new PageResult<>(List.of(USER_VIEW), 0, 5, 3), null, null);

        assertFalse(pagina.getLink(IanaLinkRelations.PREV).isPresent());
        assertFalse(pagina.getLink(IanaLinkRelations.NEXT).isPresent());
        assertTrue(href(pagina, IanaLinkRelations.FIRST).endsWith("/users?page=0&size=5"),
                "sem busca nem ordenação, os links não inventam parâmetros");
        assertEquals(href(pagina, IanaLinkRelations.FIRST), href(pagina, IanaLinkRelations.LAST));
    }

    @Test
    @DisplayName("Resultado vazio aponta a última página para a primeira, não para a página -1")
    void deveApontarAUltimaParaAPrimeiraQuandoNaoHaResultados() {
        PagedModel<EntityModel<UserResponse>> pagina = assembler.toPagedModel(
                new PageResult<>(List.of(), 0, 5, 0), null, null);

        assertTrue(href(pagina, IanaLinkRelations.LAST).contains("page=0&"));
    }

    @Test
    @DisplayName("Busca com espaço, acento e & é codificada uma única vez e não quebra a query")
    void deveCodificarABuscaUmaUnicaVez() {
        PagedModel<EntityModel<UserResponse>> pagina = assembler.toPagedModel(
                new PageResult<>(List.of(USER_VIEW), 0, 5, 1), "João & Maria", null);

        assertTrue(href(pagina, IanaLinkRelations.SELF).contains("name=Jo%C3%A3o%20%26%20Maria&page=0"));
    }

    private static String href(PagedModel<?> pagina, LinkRelation rel) {
        return pagina.getLink(rel).orElseThrow(() -> new AssertionError("sem link " + rel)).getHref();
    }
}
