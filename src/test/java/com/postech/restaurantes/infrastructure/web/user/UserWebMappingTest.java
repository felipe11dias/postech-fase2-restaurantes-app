package com.postech.restaurantes.infrastructure.web.user;

import static com.postech.restaurantes.infrastructure.web.WebFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.web.WebFixtures.NOW;
import static com.postech.restaurantes.infrastructure.web.WebFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.web.WebFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.web.WebFixtures.USER_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import com.postech.restaurantes.domain.entity.user.RoleName;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Conversão nas duas bordas do HTTP: corpo recebido → DTO do caso de uso, e view → resposta. */
class UserWebMappingTest {

    private static AddressRequest enderecoRequest() {
        return new AddressRequest("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000");
    }

    @Nested
    @DisplayName("Entrada")
    class Entrada {

        @Test
        @DisplayName("Cadastro converte papéis pelo domínio e repassa os endereços")
        void deveConverterOCadastro() {
            NewUserRequest request = new NewUserRequest("João Silva", "joao.silva@email.com", "joao.silva",
                    "senhaSegura123", Set.of("ROLE_CUSTOMER"), List.of(enderecoRequest()));

            NewUserDTO dto = request.toDTO();

            assertEquals("João Silva", dto.name());
            assertEquals("senhaSegura123", dto.password());
            assertEquals(Set.of(RoleName.ROLE_CUSTOMER), dto.roles());
            assertEquals("01001-000", dto.addresses().get(0).zipCode());
        }

        @Test
        @DisplayName("Papel desconhecido é recusado com a mensagem do domínio")
        void deveRecusarPapelDesconhecido() {
            NewUserRequest request = new NewUserRequest("João", "joao@email.com", "joao", "senhaSegura123",
                    Set.of("ROLE_INEXISTENTE"), null);

            assertThrows(IllegalArgumentException.class, request::toDTO);
        }

        @Test
        @DisplayName("Cadastro sem papéis e sem endereços chega ao caso de uso como veio")
        void deveRepassarAusencias() {
            NewUserDTO dto = new NewUserRequest("João", "joao@email.com", "joao", "senhaSegura123", null, null)
                    .toDTO();

            assertNull(dto.roles());
            assertNull(dto.addresses());
        }

        @Test
        @DisplayName("Atualização converte nome, e-mail, login e endereços")
        void deveConverterAAtualizacao() {
            UpdateUserDTO dto = new UpdateUserRequest("Novo Nome", "novo@email.com", "novo",
                    List.of(enderecoRequest())).toDTO();

            assertEquals("Novo Nome", dto.name());
            assertEquals("novo@email.com", dto.email());
            assertEquals("novo", dto.login());
            assertEquals(1, dto.addresses().size());
        }

        @Test
        @DisplayName("Troca de senha repassa as três senhas; a comparação é do caso de uso")
        void deveConverterATrocaDeSenha() {
            ChangePasswordDTO dto = new ChangePasswordRequest("atual", "novaSenha123", "novaSenha123").toDTO();

            assertEquals("atual", dto.currentPassword());
            assertEquals("novaSenha123", dto.newPassword());
            assertEquals("novaSenha123", dto.confirmPassword());
        }

        @Test
        @DisplayName("Endereço avulso converte todos os campos")
        void deveConverterOEndereco() {
            AddressDTO dto = enderecoRequest().toDTO();

            assertEquals("Rua das Flores", dto.street());
            assertEquals("100", dto.number());
            assertEquals("Apto 21", dto.complement());
            assertEquals("Centro", dto.neighborhood());
            assertEquals("São Paulo", dto.city());
            assertEquals("SP", dto.state());
            assertEquals("01001-000", dto.zipCode());
        }
    }

    @Nested
    @DisplayName("Saída")
    class Saida {

        @Test
        @DisplayName("A resposta nasce da view e não tem por onde carregar a senha")
        void deveConverterAView() {
            UserResponse response = UserResponse.from(USER_VIEW);

            assertEquals(USER_ID, response.id());
            assertEquals("João Silva", response.name());
            assertEquals("joao.silva@email.com", response.email());
            assertEquals("joao.silva", response.login());
            assertEquals(ROLE_ID, response.roles().get(0).id());
            assertEquals("ROLE_CUSTOMER", response.roles().get(0).name());
            assertEquals(ADDRESS_ID, response.addresses().get(0).id());
            assertEquals("01001000", response.addresses().get(0).zipCode());
            assertEquals(NOW.minusDays(1), response.createdAt());
            assertEquals(NOW, response.lastUpdatedAt());
            assertFalse(response.toString().toLowerCase().contains("password"));
        }
    }

    @Nested
    @DisplayName("Links de navegação")
    class Links {

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

            assertTrue(model.getLink(IanaLinkRelations.SELF).orElseThrow().getHref()
                    .endsWith(UserRestController.BASE_PATH + "/" + USER_ID));
            assertTrue(model.getLink(UserModelAssembler.USERS_REL).orElseThrow().getHref()
                    .endsWith(UserRestController.BASE_PATH));
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

        private static String href(PagedModel<?> pagina, org.springframework.hateoas.LinkRelation rel) {
            return pagina.getLink(rel).orElseThrow(() -> new AssertionError("sem link " + rel)).getHref();
        }
    }
}
