package com.postech.restaurantes.application.usecase.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.entity.role.Role;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreateRestaurantUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private IUserGateway userGateway;
    private CreateRestaurantUseCase useCase;

    private UUID userId;
    private AddressDTO endereco;
    private User owner;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        userGateway = mock(IUserGateway.class);
        useCase = CreateRestaurantUseCase.create(restaurantGateway, userGateway);

        userId = UUID.randomUUID();
        endereco = new AddressDTO("Rua A", "10", null, "Bairro", "Cidade", "SP", "01000000");
        owner = User.restore(userId, "Dono", "dono@x.com", "dono", "hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER)), List.of(), null, null);
    }

    @Test
    @DisplayName("Cria restaurante quando dono e endereço são válidos")
    void deveCriarRestauranteQuandoValido() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(restaurantGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        Restaurant result = useCase.run(dto);

        assertNotNull(result);
        assertEquals("Sabor", result.getName());
        verify(restaurantGateway).insert(any());
    }

    @Test
    @DisplayName("Aceita administrador como dono do restaurante")
    void deveCriarQuandoDonoEhAdministrador() {
        User admin = User.restore(userId, "Admin", "admin@x.com", "admin", "hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_ADMIN)), List.of(), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(admin));
        when(restaurantGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        Restaurant result = useCase.run(dto);

        assertEquals(userId, result.getUserId());
    }

    @Test
    @DisplayName("Recusa quando dono não é encontrado")
    void deveRecusarQuandoDonoNaoEncontrado() {
        when(userGateway.findById(userId)).thenReturn(Optional.empty());

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando usuário não possui papel de dono ou admin")
    void deveRecusarQuandoUsuarioSemPermissao() {
        User cliente = User.restore(userId, "Cliente", "cli@x.com", "cli", "hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER)), List.of(), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(cliente));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando o endereço do restaurante não é informado")
    void deveRecusarQuandoEnderecoAusente() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, null, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto));
        verify(restaurantGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa quando DTO é nulo")
    void deveRecusarQuandoDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
