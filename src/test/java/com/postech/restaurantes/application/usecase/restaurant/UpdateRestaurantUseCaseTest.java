package com.postech.restaurantes.application.usecase.restaurant;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.ADMIN;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLIENT;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpdateRestaurantUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private IUserGateway userGateway;
    private UpdateRestaurantUseCase useCase;

    private UUID restaurantId;
    private UUID userId;
    private AddressDTO endereco;
    private Restaurant restaurant;
    private User owner;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        userGateway = mock(IUserGateway.class);
        useCase = UpdateRestaurantUseCase.create(restaurantGateway, userGateway);

        restaurantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        endereco = new AddressDTO("Rua A", "10", null, "Bairro", "Cidade", "SP", "01000000");
        owner = User.restore(userId, "Dono", "dono@x.com", "dono", "hash",
                OWNER, List.of(), null, null);
        restaurant = Restaurant.restore(restaurantId, userId, endereco.toEntity(), "Antigo", LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);
    }

    @Test
    @DisplayName("Atualiza restaurante quando válido")
    void deveAtualizarQuandoValido() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(restaurantGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        Restaurant result = useCase.run(dto);

        assertEquals("Novo Nome", result.getName());
        assertEquals("Rua A", result.getAddress().getStreet());
        verify(restaurantGateway).update(any());
    }

    @Test
    @DisplayName("Sem dono no pedido, o dono continua o mesmo e nenhum usuário é consultado")
    void deveManterODonoQuandoNaoInformado() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, null, endereco, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        Restaurant result = useCase.run(dto);

        assertEquals(userId, result.getUserId());
        verify(userGateway, never()).findById(any());
    }

    @Test
    @DisplayName("Recusa administrador sem perfil de dono como dono do restaurante")
    void naoDeveAtualizarQuandoDonoEhSoAdministrador() {
        User admin = User.restore(userId, "Admin", "admin@x.com", "admin", "hash",
                ADMIN, List.of(), null, null);
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(admin));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        ForbiddenOperationException erro = assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));

        assertEquals("O usuário informado não tem perfil de dono de restaurante", erro.getMessage());
        verify(restaurantGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa atualização quando restaurante não existe")
    void deveRecusarQuandoRestauranteNaoExiste() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.empty());

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa atualização quando dono não existe")
    void deveRecusarQuandoDonoNaoExiste() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.empty());

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa atualização quando usuário não tem permissão")
    void deveRecusarQuandoUsuarioSemPermissao() {
        User cliente = User.restore(userId, "Cliente", "cli@x.com", "cli", "hash",
                CLIENT, List.of(), null, null);
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(cliente));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando o endereço do restaurante não é informado")
    void deveRecusarQuandoEnderecoAusente() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, null, "Novo Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto));
        verify(restaurantGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa DTO nulo ou ID nulo")
    void deveRecusarDtoOuIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
        UpdateRestaurantDTO dtoSemId = new UpdateRestaurantDTO(null, userId, endereco, "Nome", LocalTime.of(9, 0), LocalTime.of(23, 0));
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dtoSemId));
    }
}
