package com.postech.restaurantes.infrastructure.api.rest.spring.dto;

import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.NOW;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.api.rest.spring.WebFixtures.USER_VIEW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.adapter.presenter.view.AdminProfileView;
import com.postech.restaurantes.adapter.presenter.view.CourierProfileView;
import com.postech.restaurantes.adapter.presenter.view.OwnerProfileView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.user.ChangePasswordDTO;
import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import com.postech.restaurantes.application.dto.user.CourierProfileDTO;
import com.postech.restaurantes.application.dto.user.NewUserDTO;
import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import com.postech.restaurantes.application.dto.user.UpdateUserDTO;
import com.postech.restaurantes.application.dto.user.UserAddressDTO;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.AddressRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ChangePasswordRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ClientProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.CourierProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.NewUserRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.OwnerProfileRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UpdateUserRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.UserAddressRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.UserResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** DTOs HTTP: corpo recebido → DTO do caso de uso (request), e view → resposta (response). */
class UserDtoMappingTest {

    private static AddressRequest enderecoRequest() {
        return new AddressRequest("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000");
    }

    private static UserAddressRequest enderecoDoUsuarioRequest() {
        return new UserAddressRequest(null, "Casa", true, enderecoRequest());
    }

    @Nested
    @DisplayName("Entrada")
    class Entrada {

        @Test
        @DisplayName("Cadastro repassa os perfis e os endereços ao caso de uso")
        void deveConverterOCadastro() {
            NewUserRequest request = new NewUserRequest("João Silva", "joao.silva@email.com", "joao.silva",
                    "senhaSegura123", new OwnerProfileRequest("11.222.333/0001-81", "Sabor Ltda", "(11) 3123-4567"),
                    new ClientProfileRequest("529.982.247-25", "(11) 91234-5678", LocalDate.of(1990, 5, 20)),
                    new CourierProfileRequest("529.982.247-25", "(11) 91234-5678", "MOTORCYCLE", "02650306461",
                            "ABC1D23"),
                    List.of(enderecoDoUsuarioRequest()));

            NewUserDTO dto = request.toDTO();

            assertEquals("João Silva", dto.name());
            assertEquals("senhaSegura123", dto.password());
            assertEquals(new OwnerProfileDTO("11.222.333/0001-81", "Sabor Ltda", "(11) 3123-4567"), dto.owner());
            assertEquals(new ClientProfileDTO("529.982.247-25", "(11) 91234-5678", LocalDate.of(1990, 5, 20)),
                    dto.client());
            assertEquals(new CourierProfileDTO("529.982.247-25", "(11) 91234-5678", "MOTORCYCLE", "02650306461",
                    "ABC1D23"), dto.courier());
            assertEquals("Casa", dto.addresses().get(0).label());
            assertEquals(Boolean.TRUE, dto.addresses().get(0).isDefault());
            assertEquals("01001-000", dto.addresses().get(0).address().zipCode());
        }

        @Test
        @DisplayName("Cadastro sem perfis e sem endereços chega ao caso de uso como veio")
        void deveRepassarAusencias() {
            NewUserDTO dto = new NewUserRequest("João", "joao@email.com", "joao", "senhaSegura123", null, null,
                    null, null).toDTO();

            assertNull(dto.owner());
            assertNull(dto.client());
            assertNull(dto.courier());
            assertNull(dto.addresses());
        }

        @Test
        @DisplayName("Atualização converte nome, e-mail, login e endereços")
        void deveConverterAAtualizacao() {
            UpdateUserDTO dto = new UpdateUserRequest("Novo Nome", "novo@email.com", "novo",
                    List.of(enderecoDoUsuarioRequest())).toDTO();

            assertEquals("Novo Nome", dto.name());
            assertEquals("novo@email.com", dto.email());
            assertEquals("novo", dto.login());
            assertEquals(1, dto.addresses().size());
        }

        @Test
        @DisplayName("Endereço do usuário sem o endereço aninhado chega ao caso de uso sem ele, com o id do corpo")
        void deveRepassarEnderecoDoUsuarioSemEndereco() {
            UserAddressDTO dto = new UserAddressRequest(USER_ADDRESS_ID, "Casa", null, null).toDTO();

            assertEquals(USER_ADDRESS_ID, dto.id());
            assertEquals("Casa", dto.label());
            assertNull(dto.isDefault());
            assertNull(dto.address());
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
            assertEquals(List.of("ROLE_CLIENT"), response.roles());
            assertEquals("52998224725", response.client().cpf());
            assertEquals("11912345678", response.client().phone());
            assertNull(response.client().birthDate());
            assertNull(response.owner());
            assertNull(response.courier());
            assertNull(response.admin());
            assertEquals(USER_ADDRESS_ID, response.addresses().get(0).id());
            assertEquals("Casa", response.addresses().get(0).label());
            assertTrue(response.addresses().get(0).isDefault());
            assertEquals(ADDRESS_ID, response.addresses().get(0).address().id());
            assertEquals("01001000", response.addresses().get(0).address().zipCode());
            assertEquals(NOW.minusDays(1), response.createdAt());
            assertEquals(NOW, response.lastUpdatedAt());
            assertFalse(response.toString().toLowerCase().contains("password"));
        }

        @Test
        @DisplayName("Os perfis de dono, entregador e administrador saem da view com todos os campos")
        void deveConverterOsDemaisPerfis() {
            UserView view = new UserView(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva",
                    List.of("ROLE_OWNER", "ROLE_COURIER", "ROLE_ADMIN"),
                    new OwnerProfileView("11222333000181", "Sabor Ltda", "1131234567"), null,
                    new CourierProfileView("52998224725", "11912345678", "CAR", "02650306461", "ABC1D23", "BUSY"),
                    new AdminProfileView("ADM-1", "Operações", true), List.of(), NOW, NOW);

            UserResponse response = UserResponse.from(view);

            assertEquals("11222333000181", response.owner().cnpj());
            assertEquals("Sabor Ltda", response.owner().legalName());
            assertEquals("1131234567", response.owner().businessPhone());
            assertNull(response.client());
            assertEquals("52998224725", response.courier().cpf());
            assertEquals("11912345678", response.courier().phone());
            assertEquals("CAR", response.courier().vehicleType());
            assertEquals("02650306461", response.courier().driverLicense());
            assertEquals("ABC1D23", response.courier().vehiclePlate());
            assertEquals("BUSY", response.courier().status());
            assertEquals("ADM-1", response.admin().employeeCode());
            assertEquals("Operações", response.admin().department());
            assertTrue(response.admin().superAdmin());
        }
    }
}
