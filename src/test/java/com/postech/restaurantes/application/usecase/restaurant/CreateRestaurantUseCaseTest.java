package com.postech.restaurantes.application.usecase.restaurant;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OFFICE_HOURS_DTO;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.ADMIN;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.CLIENT;
import static com.postech.restaurantes.application.usecase.UseCaseFixtures.OWNER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.restaurant.CreateRestaurantDTO;
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

class CreateRestaurantUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private IUserGateway userGateway;
    private ICuisineGateway cuisineGateway;
    private CreateRestaurantUseCase useCase;

    private UUID userId;
    private AddressDTO endereco;
    private User owner;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        userGateway = mock(IUserGateway.class);
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = CreateRestaurantUseCase.create(restaurantGateway, userGateway, cuisineGateway);

        userId = UUID.randomUUID();
        endereco = new AddressDTO("Rua A", "10", null, "Bairro", "Cidade", "SP", "01000000");
        owner = User.restore(userId, "Dono", "dono@x.com", "dono", "hash",
                OWNER, List.of(), null, null);
    }

    @Test
    @DisplayName("Cria restaurante quando dono e endereço são válidos, sem tipo de cozinha se nenhum for pedido")
    void deveCriarRestauranteQuandoValido() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(restaurantGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO, null);
        Restaurant result = useCase.run(dto);

        assertNotNull(result);
        assertEquals("Sabor", result.getName());
        assertTrue(result.getCuisines().isEmpty());
        verify(restaurantGateway).insert(any());
        verifyNoInteractions(cuisineGateway);
    }

    @Test
    @DisplayName("Cria restaurante com os tipos de cozinha do catálogo, em ordem alfabética")
    void deveCriarComTiposDeCozinha() {
        Cuisine japonesa = Cuisine.restore(UUID.randomUUID(), "Japonesa", null, null);
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(cuisineGateway.findAllById(List.of(japonesa.getId(), italiana.getId())))
                .thenReturn(List.of(japonesa, italiana));
        when(restaurantGateway.insert(any())).thenAnswer(inv -> inv.getArgument(0));

        Restaurant result = useCase.run(new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO,
                List.of(japonesa.getId(), italiana.getId())));

        assertEquals(List.of("Italiana", "Japonesa"), result.getCuisines().stream().map(Cuisine::getName).toList());
    }

    @Test
    @DisplayName("Recusa tipo de cozinha que não está no catálogo, sem gravar")
    void deveRecusarTipoDeCozinhaInexistente() {
        UUID desconhecido = UUID.randomUUID();
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));
        when(cuisineGateway.findAllById(List.of(desconhecido))).thenReturn(List.of());

        CreateRestaurantDTO dto =
                new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO, List.of(desconhecido));
        ResourceNotFoundException erro = assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));

        assertEquals("Tipo de cozinha não encontrado", erro.getMessage());
        verify(restaurantGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa administrador sem perfil de dono como dono do restaurante")
    void naoDeveCriarQuandoDonoEhSoAdministrador() {
        User admin = User.restore(userId, "Admin", "admin@x.com", "admin", "hash",
                ADMIN, List.of(), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(admin));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO, null);
        ForbiddenOperationException erro = assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));

        assertEquals("O usuário informado não tem perfil de dono de restaurante", erro.getMessage());
        verify(restaurantGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa quando dono não é encontrado")
    void deveRecusarQuandoDonoNaoEncontrado() {
        when(userGateway.findById(userId)).thenReturn(Optional.empty());

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO, null);
        assertThrows(ResourceNotFoundException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando usuário não possui papel de dono ou admin")
    void deveRecusarQuandoUsuarioSemPermissao() {
        User cliente = User.restore(userId, "Cliente", "cli@x.com", "cli", "hash",
                CLIENT, List.of(), null, null);
        when(userGateway.findById(userId)).thenReturn(Optional.of(cliente));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, endereco, "Sabor", OFFICE_HOURS_DTO, null);
        assertThrows(ForbiddenOperationException.class, () -> useCase.run(dto));
    }

    @Test
    @DisplayName("Recusa quando o endereço do restaurante não é informado")
    void deveRecusarQuandoEnderecoAusente() {
        when(userGateway.findById(userId)).thenReturn(Optional.of(owner));

        CreateRestaurantDTO dto = new CreateRestaurantDTO(userId, null, "Sabor", OFFICE_HOURS_DTO, null);
        assertThrows(IllegalArgumentException.class, () -> useCase.run(dto));
        verify(restaurantGateway, never()).insert(any());
    }

    @Test
    @DisplayName("Recusa quando DTO é nulo")
    void deveRecusarQuandoDtoNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
