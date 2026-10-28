package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.LocationFixtures;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OFFICE_HOURS;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OFFICE_HOURS_DTO;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.ADMIN;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLIENT;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.restaurant.UpdateRestaurantDTO;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
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

class UpdateRestaurantUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private IUserGateway userGateway;
    private ICuisineGateway cuisineGateway;
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
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = UpdateRestaurantUseCase.create(restaurantGateway, userGateway, cuisineGateway);

        restaurantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        endereco = new AddressDTO(LocationFixtures.cityId("Cidade", "SP"), "Rua A", "10", null, "Bairro", "01000000");
        owner = User.restore(userId, "Dono", "dono@x.com", "dono", "hash",
                OWNER, List.of(), null, null);
        restaurant = Restaurant.restore(restaurantId, userId, endereco.toEntity(), "Antigo", OFFICE_HOURS, List.of(), null, null);
    }

    @Test
    @DisplayName("Atualiza restaurante quando válido")
    void deveAtualizarQuandoValido() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(restaurantGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", OFFICE_HOURS_DTO, null);
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

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, null, endereco, "Novo Nome", OFFICE_HOURS_DTO, null);
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

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", OFFICE_HOURS_DTO, null);
        ForbiddenOperationException erro = assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));

        assertEquals("O usuário informado não tem perfil de dono de restaurante", erro.getMessage());
        verify(restaurantGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa atualização quando restaurante não existe")
    void deveRecusarQuandoRestauranteNaoExiste() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.empty());

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", OFFICE_HOURS_DTO, null);
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa atualização quando dono não existe")
    void deveRecusarQuandoDonoNaoExiste() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.empty());

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", OFFICE_HOURS_DTO, null);
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa atualização quando usuário não tem permissão")
    void deveRecusarQuandoUsuarioSemPermissao() {
        User cliente = User.restore(userId, "Cliente", "cli@x.com", "cli", "hash",
                CLIENT, List.of(), null, null);
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(cliente));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, endereco, "Novo Nome", OFFICE_HOURS_DTO, null);
        assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando o endereço do restaurante não é informado")
    void deveRecusarQuandoEnderecoAusente() {
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, userId, null, "Novo Nome", OFFICE_HOURS_DTO, null);
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto));
        verify(restaurantGateway, never()).update(any());
    }

    @Test
    @DisplayName("Sem tipos de cozinha no pedido, os que o restaurante já tem continuam e o catálogo não é consultado")
    void deveManterOsTiposDeCozinhaQuandoNaoInformados() {
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        restaurant.replaceCuisines(List.of(italiana));
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Restaurant result = useCase.run(
                new UpdateRestaurantDTO(restaurantId, null, endereco, "Novo Nome", OFFICE_HOURS_DTO, null));

        assertEquals(List.of(italiana), result.getCuisines());
        verifyNoInteractions(cuisineGateway);
    }

    @Test
    @DisplayName("Com tipos de cozinha no pedido, a lista inteira é trocada pelos do catálogo")
    void deveTrocarOsTiposDeCozinha() {
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        Cuisine japonesa = Cuisine.restore(UUID.randomUUID(), "Japonesa", null, null);
        restaurant.replaceCuisines(List.of(italiana));
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cuisineGateway.findAllById(List.of(japonesa.getId()))).thenReturn(List.of(japonesa));
        when(restaurantGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Restaurant result = useCase.run(new UpdateRestaurantDTO(restaurantId, null, endereco, "Novo Nome",
                OFFICE_HOURS_DTO, List.of(japonesa.getId())));

        assertEquals(List.of(japonesa), result.getCuisines());
    }

    @Test
    @DisplayName("Lista vazia de tipos de cozinha tira todos")
    void deveTirarTodosOsTiposDeCozinha() {
        restaurant.replaceCuisines(List.of(Cuisine.restore(UUID.randomUUID(), "Italiana", null, null)));
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cuisineGateway.findAllById(List.of())).thenReturn(List.of());
        when(restaurantGateway.update(any())).thenAnswer(inv -> inv.getArgument(0));

        Restaurant result = useCase.run(
                new UpdateRestaurantDTO(restaurantId, null, endereco, "Novo Nome", OFFICE_HOURS_DTO, List.of()));

        assertTrue(result.getCuisines().isEmpty());
    }

    @Test
    @DisplayName("Recusa tipo de cozinha que não está no catálogo, sem gravar")
    void deveRecusarTipoDeCozinhaInexistente() {
        UUID desconhecido = UUID.randomUUID();
        when(restaurantGateway.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cuisineGateway.findAllById(List.of(desconhecido))).thenReturn(List.of());

        UpdateRestaurantDTO dto = new UpdateRestaurantDTO(restaurantId, null, endereco, "Novo Nome",
                OFFICE_HOURS_DTO, List.of(desconhecido));
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));

        verify(restaurantGateway, never()).update(any());
    }

    @Test
    @DisplayName("Recusa DTO nulo ou ID nulo")
    void deveRecusarDtoOuIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
        UpdateRestaurantDTO dtoSemId = new UpdateRestaurantDTO(null, userId, endereco, "Nome", OFFICE_HOURS_DTO, null);
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dtoSemId));
    }
}
