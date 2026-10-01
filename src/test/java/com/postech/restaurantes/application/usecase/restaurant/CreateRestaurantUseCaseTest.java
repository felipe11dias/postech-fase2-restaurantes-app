package com.postech.restaurantes.application.usecase.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.RoleName;
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
    private UUID addressId;
    private Address address;
    private User owner;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        userGateway = mock(IUserGateway.class);
        useCase = CreateRestaurantUseCase.create(restaurantGateway, userGateway);

        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        address = Address.restore(addressId, "Rua A", "10", null, "Bairro", "Cidade", "SP", "01000000");
        owner = User.restore(userId, "Dono", "dono@x.com", "dono", "hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER)), List.of(address), null, null);
    }

    @Test
    @DisplayName("Cria restaurante quando dono e endereço são válidos")
    void deveCriarRestauranteQuandoValido() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(restaurantGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        Restaurant result = useCase.run(dto);

        assertNotNull(result);
        assertEquals("Sabor", result.getName());
        verify(restaurantGateway).insert(any());
    }

    @Test
    @DisplayName("Recusa quando dono não é encontrado")
    void deveRecusarQuandoDonoNaoEncontrado() {
        when(userGateway.findById(userId)).thenReturn(Optional.empty());

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando usuário não possui papel de dono ou admin")
    void deveRecusarQuandoUsuarioSemPermissao() {
        User cliente = User.restore(userId, "Cliente", "cli@x.com", "cli", "hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER)), List.of(address), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(cliente));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando endereço não pertence ao usuário")
    void deveRecusarQuandoEnderecoNaoPertenceAoUsuario() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, UUID.randomUUID(), "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0));
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando DTO é nulo")
    void deveRecusarQuandoDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
