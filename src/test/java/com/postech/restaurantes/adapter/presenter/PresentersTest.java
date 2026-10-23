package com.postech.restaurantes.adapter.presenter;

import static com.postech.restaurantes.adapter.AdapterFixtures.ADDRESS_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.ADMIN_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.CLIENT_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.COURIER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.HASH;
import static com.postech.restaurantes.adapter.AdapterFixtures.OWNER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ADDRESS_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.gateway.UserGateway;
import com.postech.restaurantes.adapter.presenter.view.AuthView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PresentersTest {

    /** Entidade obtida pelo caminho público: origem de dados (mock) → gateway → domínio. */
    private static User userFromData() {
        IUserDataSource dataSource = org.mockito.Mockito.mock(IUserDataSource.class);
        org.mockito.Mockito.when(dataSource.findById(USER_ID)).thenReturn(Optional.of(USER_DATA));
        return UserGateway.create(dataSource).findById(USER_ID).orElseThrow();
    }

    @Test
    @DisplayName("UserPresenter expõe todos os dados públicos e nunca o hash da senha")
    void deveApresentarUsuarioSemSenha() {
        User user = userFromData();

        UserView view = UserPresenter.toView(user);

        assertEquals(USER_ID, view.id());
        assertEquals("João Silva", view.name());
        assertEquals("joao.silva@email.com", view.email());
        assertEquals("joao.silva", view.login());
        assertEquals(List.of("ROLE_CLIENT"), view.roles());
        assertEquals("52998224725", view.client().cpf());
        assertNull(view.owner());
        assertNull(view.courier());
        assertNull(view.admin());
        assertEquals(USER_ADDRESS_ID, view.addresses().get(0).id());
        assertEquals("Casa", view.addresses().get(0).label());
        assertTrue(view.addresses().get(0).isDefault());
        assertEquals(ADDRESS_ID, view.addresses().get(0).address().id());
        assertEquals("01001000", view.addresses().get(0).address().zipCode());
        assertEquals("SP", view.addresses().get(0).address().state());
        assertEquals(NOW.minusDays(1), view.createdAt());
        assertEquals(NOW, view.lastUpdatedAt());
        assertFalse(view.toString().contains(USER_DATA.passwordHash()));
    }

    @Test
    @DisplayName("UserPresenter expõe os quatro perfis e os papéis derivados deles, na ordem fixa")
    void deveApresentarTodosOsPerfis() {
        IUserDataSource dataSource = org.mockito.Mockito.mock(IUserDataSource.class);
        org.mockito.Mockito.when(dataSource.findById(USER_ID)).thenReturn(Optional.of(new UserData(USER_ID,
                "João Silva", "joao.silva@email.com", "joao.silva", HASH, OWNER_DATA, CLIENT_DATA, COURIER_DATA,
                ADMIN_DATA, List.of(), NOW, NOW)));

        UserView view = UserPresenter.toView(UserGateway.create(dataSource).findById(USER_ID).orElseThrow());

        assertEquals(List.of("ROLE_OWNER", "ROLE_CLIENT", "ROLE_COURIER", "ROLE_ADMIN"), view.roles());
        assertEquals("11222333000181", view.owner().cnpj());
        assertEquals("Sabor Ltda", view.owner().legalName());
        assertEquals("1131234567", view.owner().businessPhone());
        assertEquals("11912345678", view.client().phone());
        assertNull(view.client().birthDate());
        assertEquals("MOTORCYCLE", view.courier().vehicleType());
        assertEquals("02650306461", view.courier().driverLicense());
        assertEquals("ABC1D23", view.courier().vehiclePlate());
        assertEquals("AVAILABLE", view.courier().status());
        assertEquals("ADM-1", view.admin().employeeCode());
        assertEquals("Operações", view.admin().department());
        assertTrue(view.admin().superAdmin());
    }

    @Test
    @DisplayName("UserPresenter mostra entregador a pé sem CNH nem placa")
    void deveApresentarEntregadorSemDocumentosDeVeiculo() {
        IUserDataSource dataSource = org.mockito.Mockito.mock(IUserDataSource.class);
        org.mockito.Mockito.when(dataSource.findById(USER_ID)).thenReturn(Optional.of(new UserData(USER_ID,
                "João Silva", "joao.silva@email.com", "joao.silva", HASH, null, null,
                new CourierData("52998224725", "11912345678",
                        "BICYCLE", null, null, "OFFLINE"),
                null, List.of(), NOW, NOW)));

        UserView view = UserPresenter.toView(UserGateway.create(dataSource).findById(USER_ID).orElseThrow());

        assertNull(view.courier().driverLicense());
        assertNull(view.courier().vehiclePlate());
        assertEquals(List.of("ROLE_COURIER"), view.roles());
    }

    @Test
    @DisplayName("UserPresenter mapeia uma página preservando os metadados")
    void deveApresentarPagina() {
        PageResult<User> page = new PageResult<>(List.of(userFromData()), 2, 5, 11);

        PageResult<UserView> view = UserPresenter.toView(page);

        assertEquals(1, view.content().size());
        assertEquals(USER_ID, view.content().get(0).id());
        assertEquals(2, view.page());
        assertEquals(5, view.size());
        assertEquals(11, view.totalElements());
    }

    @Test
    @DisplayName("UserPresenter recusa entrada nula")
    void deveRecusarNulo() {
        assertThrows(IllegalArgumentException.class, () -> UserPresenter.toView((User) null));
        assertThrows(IllegalArgumentException.class, () -> UserPresenter.toView((PageResult<User>) null));
    }

    @Test
    @DisplayName("AuthPresenter produz token, esquema Bearer e expiração")
    void deveApresentarAutenticacao() {
        AuthView view = AuthPresenter.toView(new IssuedToken("jwt", NOW.plusHours(1)));

        assertEquals("jwt", view.token());
        assertEquals("Bearer", view.type());
        assertEquals(NOW.plusHours(1), view.expiresAt());
    }

    @Test
    @DisplayName("AuthPresenter recusa token nulo")
    void deveRecusarTokenNulo() {
        assertThrows(IllegalArgumentException.class, () -> AuthPresenter.toView(null));
    }
}
