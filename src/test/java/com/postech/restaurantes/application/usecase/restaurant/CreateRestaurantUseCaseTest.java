package com.postech.restaurantes.application.usecase.restaurant;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OFFICE_HOURS_DTO;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.ADMIN;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLIENT;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OWNER;
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
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
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
                OWNER, List.of(), null, null);
    }

    @Test
    @DisplayName("Cria restaurante quando dono e endereço são válidos")
    void deveCriarRestauranteQuandoValido() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(restaurantGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO);
        Restaurant result = useCase.run(dto);

        assertNotNull(result);
        assertEquals("Sabor", result.getName());
        verify(restaurantGateway).insert(any());
    }

    @Test
    @DisplayName("Recusa administrador sem perfil de dono como dono do restaurante")
    void naoDeveCriarQuandoDonoEhSoAdministrador() {
        User admin = User.restore(userId, "Admin", "admin@x.com", "admin", "hash",
                ADMIN, List.of(), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(admin));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO);
        ForbiddenOperationException erro = assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));

        assertEquals("O usuário informado não tem perfil de dono de restaurante", erro.getMessage());
        verify(restaurantGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa quando dono não é encontrado")
    void deveRecusarQuandoDonoNaoEncontrado() {
        when(userGateway.findById(userId)).thenReturn(Optional.empty());

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO);
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando usuário não possui papel de dono ou admin")
    void deveRecusarQuandoUsuarioSemPermissao() {
        User cliente = User.restore(userId, "Cliente", "cli@x.com", "cli", "hash",
                CLIENT, List.of(), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(cliente));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO);
        assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando o endereço do restaurante não é informado")
    void deveRecusarQuandoEnderecoAusente() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, null, "Sabor", OFFICE_HOURS_DTO);
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto));
        verify(restaurantGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa quando DTO é nulo")
    void deveRecusarQuandoDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
