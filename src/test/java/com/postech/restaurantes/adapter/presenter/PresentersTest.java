package com.postech.restaurantes.adapter.presenter;

import static com.postech.restaurantes.adapter.AdapterFixtures.ADDRESS_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.ROLE_ID;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
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
        assertEquals(ROLE_ID, view.roles().get(0).id());
        assertEquals("ROLE_CUSTOMER", view.roles().get(0).name());
        assertEquals(ADDRESS_ID, view.addresses().get(0).id());
        assertEquals("01001000", view.addresses().get(0).zipCode());
        assertEquals("SP", view.addresses().get(0).state());
        assertEquals(NOW.minusDays(1), view.createdAt());
        assertEquals(NOW, view.lastUpdatedAt());
        assertFalse(view.toString().contains(USER_DATA.passwordHash()));
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
