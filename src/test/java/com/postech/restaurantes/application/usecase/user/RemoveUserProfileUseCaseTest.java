package com.postech.restaurantes.application.usecase.user;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.USER_ID;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.existingUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ResourceInUseException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RemoveUserProfileUseCaseTest {

    private IUserGateway userGateway;
    private IRestaurantGateway restaurantGateway;
    private RemoveUserProfileUseCase useCase;
    private User clienteEDono;

    @BeforeEach
    void setUp() {
        userGateway = mock(IUserGateway.class);
        restaurantGateway = mock(IRestaurantGateway.class);
        useCase = RemoveUserProfileUseCase.create(userGateway, restaurantGateway);
        clienteEDono = existingUser();
        clienteEDono.replaceProfiles(clienteEDono.getProfiles()
                .withOwner(OwnerProfile.restore("11222333000181", "Sabor Ltda", "1131234567")));
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(clienteEDono));
        when(userGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Remove o perfil de cliente e fica com o de dono")
    void deveRemoverUmPerfil() {
        User result = useCase.run(USER_ID, "client");

        assertNull(result.getProfiles().client());
        assertEquals(Set.of(RoleName.ROLE_OWNER), result.getRoles());
        verify(userGateway).update(clienteEDono);
        verify(restaurantGateway, never()).existsByUserId(any());
    }

    @Test
    @DisplayName("Remove o perfil de dono de quem não tem restaurante")
    void deveRemoverDonoSemRestaurante() {
        when(restaurantGateway.existsByUserId(USER_ID)).thenReturn(false);

        User result = useCase.run(USER_ID, "OWNER");

        assertEquals(Set.of(RoleName.ROLE_CLIENT), result.getRoles());
    }

    @Test
    @DisplayName("Não remove o perfil de dono enquanto houver restaurante do usuário")
    void naoDeveRemoverDonoComRestaurante() {
        when(restaurantGateway.existsByUserId(USER_ID)).thenReturn(true);

        ResourceInUseException erro = assertThrows(ResourceInUseException.class, () -> useCase.run(USER_ID, "owner"));

        assertEquals("O perfil de dono não pode ser removido enquanto o usuário tiver restaurantes", erro.getMessage());
        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Perfil que o usuário não tem: 404 com o nome do perfil")
    void deveRecusarPerfilQueOUsuarioNaoTem() {
        ResourceNotFoundException erro =
                assertThrows(ResourceNotFoundException.class, () -> useCase.run(USER_ID, "courier"));

        assertEquals("O usuário não tem perfil de entregador", erro.getMessage());
    }

    @Test
    @DisplayName("Não remove o último perfil (regra do domínio)")
    void naoDeveRemoverOUltimoPerfil() {
        User soCliente = existingUser();
        when(userGateway.findById(USER_ID)).thenReturn(Optional.of(soCliente));

        IllegalArgumentException erro =
                assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, "client"));

        assertEquals("Usuário deve ter ao menos um perfil", erro.getMessage());
        verify(userGateway, never()).update(any());
    }

    @Test
    @DisplayName("Tipo desconhecido é recusado antes de consultar o banco")
    void deveRecusarTipoDesconhecido() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(USER_ID, "gerente"));

        verify(userGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Usuário inexistente: 404; id nulo recusado")
    void deveRecusarUsuarioInexistente() {
        when(userGateway.findById(USER_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(USER_ID, "client"));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null, "client"));
    }

    @Test
    @DisplayName("O último administrador não perde o perfil de administrador; havendo outro, perde")
    void naoDeveTirarOAdministradorDoUltimoAdministrador() {
        clienteEDono.replaceProfiles(clienteEDono.getProfiles().withAdmin(AdminProfile.restore("ADM-1", null, true)));
        when(userGateway.countAdmins()).thenReturn(1L);

        ResourceInUseException erro = assertThrows(ResourceInUseException.class, () -> useCase.run(USER_ID, "admin"));
        when(userGateway.countAdmins()).thenReturn(2L);
        User result = useCase.run(USER_ID, "admin");

        assertEquals("O último administrador não pode deixar de ser administrador", erro.getMessage());
        assertNull(result.getProfiles().admin());
    }
}
