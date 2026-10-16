package com.postech.restaurantes.application.policy.user;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LastAdminPolicyTest {

    private IUserGateway userGateway;
    private LastAdminPolicy policy;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        policy = LastAdminPolicy.create(userGateway);
    }

    @Test
    @DisplayName("Recusa com a mensagem da operação quando o usuário é o último administrador")
    void deveRecusarOUltimoAdministrador() {
        when(userGateway.countAdmins()).thenReturn(1L);

        ResourceInUseException erro =
                assertThrows(ResourceInUseException.class, () -> policy.requireNotLastAdmin(administrador(), "Recusado"));

        assertEquals("Recusado", erro.getMessage());
    }

    @Test
    @DisplayName("Aceita o administrador quando há outro")
    void deveAceitarQuandoHaOutroAdministrador() {
        when(userGateway.countAdmins()).thenReturn(2L);

        assertDoesNotThrow(() -> policy.requireNotLastAdmin(administrador(), "Recusado"));
    }

    @Test
    @DisplayName("Quem não é administrador passa sem consultar quantos há")
    void naoDeveConsultarQuandoNaoEAdministrador() {
        assertDoesNotThrow(() -> policy.requireNotLastAdmin(existingUser(), "Recusado"));

        verify(userGateway, never()).countAdmins();
    }

    private static User administrador() {
        User user = existingUser();
        user.replaceProfiles(user.getProfiles().withAdmin(AdminProfile.restore("ADM-1", null, true)));
        return user;
    }
}
