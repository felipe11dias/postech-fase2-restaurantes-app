package com.postech.restaurantes.infrastructure.api.rest.spring.dto;

import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.NOW;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.AddressRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ChangePasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.NewUserRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateUserRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.UserResponse;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** DTOs HTTP: corpo recebido → DTO do caso de uso (request), e view → resposta (response). */
class UserDtoMappingTest {

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
}
