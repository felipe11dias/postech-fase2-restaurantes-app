package com.postech.restaurantes.infrastructure.web.user;

import static com.postech.restaurantes.infrastructure.web.WebFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.web.WebFixtures.USER_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.controller.UserController;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
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

/**
 * A porta HTTP é testada como objeto: prova que ela delega ao controller de adaptação e monta
 * a resposta certa. Quem exercita o roteamento, a serialização e a autorização de verdade são
 * os testes de integração — aqui isso seria testar o Spring, não o código do projeto.
 */
class UserRestControllerTest {

    private UserController controller;
    private UserRestController restController;

    @BeforeEach
    void setUp() {
        controller = mock(UserController.class);
        restController = new UserRestController(controller, new UserModelAssembler());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void fecharRequisicao() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Cadastro responde 201 com o Location apontando para o recurso criado")
    void deveResponder201NoCadastro() {
        when(controller.register(any())).thenReturn(USER_VIEW);
        NewUserRequest request = new NewUserRequest("João Silva", "joao.silva@email.com", "joao.silva",
                "senhaSegura123", Set.of("ROLE_CUSTOMER"), null);

        ResponseEntity<EntityModel<UserResponse>> resposta = restController.register(request);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertTrue(resposta.getHeaders().getLocation().toString()
                .endsWith(UserRestController.BASE_PATH + "/" + USER_ID));
        assertEquals(USER_ID, resposta.getBody().getContent().id());
        ArgumentCaptor<NewUserDTO> captor = ArgumentCaptor.forClass(NewUserDTO.class);
        verify(controller).register(captor.capture());
        assertEquals("joao.silva", captor.getValue().login());
    }

    @Test
    @DisplayName("Consulta por id devolve a representação com links")
    void deveConsultarPorId() {
        when(controller.findById(USER_ID)).thenReturn(USER_VIEW);

        EntityModel<UserResponse> resposta = restController.findById(USER_ID);

        assertEquals("João Silva", resposta.getContent().name());
        assertTrue(resposta.getLinks().hasSize(2));
    }

    @Test
    @DisplayName("Busca repassa nome e paginação e devolve a página com metadados")
    void deveBuscar() {
        when(controller.search(any(), any())).thenReturn(new PageResult<>(List.of(USER_VIEW), 1, 5, 11));

        PagedModel<EntityModel<UserResponse>> pagina = restController.search("jo", 1, 5, "name,desc");

        assertEquals(11, pagina.getMetadata().getTotalElements());
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(controller).search(org.mockito.ArgumentMatchers.eq("jo"), captor.capture());
        assertEquals(1, captor.getValue().page());
        assertEquals(5, captor.getValue().size());
        assertEquals("name", captor.getValue().sortBy());
        assertEquals(SortDirection.DESC, captor.getValue().direction());
    }

    @Test
    @DisplayName("Atualização devolve a representação atualizada")
    void deveAtualizar() {
        when(controller.update(any(), any())).thenReturn(USER_VIEW);

        EntityModel<UserResponse> resposta = restController.update(USER_ID,
                new UpdateUserRequest("Novo Nome", "novo@email.com", "novo", null));

        assertEquals(USER_ID, resposta.getContent().id());
        ArgumentCaptor<UpdateUserDTO> captor = ArgumentCaptor.forClass(UpdateUserDTO.class);
        verify(controller).update(org.mockito.ArgumentMatchers.eq(USER_ID), captor.capture());
        assertEquals("Novo Nome", captor.getValue().name());
    }

    @Test
    @DisplayName("Troca de senha e exclusão delegam e não devolvem corpo")
    void deveDelegarOperacoesSemCorpo() {
        restController.changePassword(USER_ID, new ChangePasswordRequest("atual", "novaSenha123", "novaSenha123"));
        restController.delete(USER_ID);

        verify(controller).changePassword(org.mockito.ArgumentMatchers.eq(USER_ID), any());
        verify(controller).delete(USER_ID);
    }

    @Test
    @DisplayName("Sem o parâmetro de ordenação, a página vai sem ordenação pedida")
    void deveAceitarBuscaSemOrdenacao() {
        assertNull(UserRestController.paginacao(0, 20, null).sortBy());
        assertNull(UserRestController.paginacao(0, 20, "   ").sortBy());
    }

    @Test
    @DisplayName("Ordenação sem direção explícita é crescente")
    void deveAssumirOrdemCrescente() {
        PageRequest pedido = UserRestController.paginacao(0, 20, "createdAt");

        assertEquals("createdAt", pedido.sortBy());
        assertEquals(SortDirection.ASC, pedido.direction());
    }

    @Test
    @DisplayName("A direção é lida sem diferenciar maiúsculas; valor estranho vira crescente")
    void deveLerADirecao() {
        assertEquals(SortDirection.DESC, UserRestController.paginacao(0, 20, "name,DESC").direction());
        assertEquals(SortDirection.ASC, UserRestController.paginacao(0, 20, "name,qualquer").direction());
    }
}
