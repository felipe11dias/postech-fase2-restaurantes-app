package com.postech.restaurantes.application.usecase.restaurant;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.address;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeleteRestaurantUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private DeleteRestaurantUseCase useCase;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        useCase = DeleteRestaurantUseCase.create(restaurantGateway);
    }

    @Test
    @DisplayName("Exclui restaurante quando id existe")
    void deveExcluirQuandoExiste() {
        UUID id = UUID.randomUUID();
        Restaurant r = Restaurant.restore(id, UUID.randomUUID(), address(), "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);
        when(restaurantGateway.findById(id)).thenReturn(Optional.of(r));

        useCase.run(id);

        verify(restaurantGateway).delete(id);
    }

    @Test
    @DisplayName("Lança exceção quando id não existe")
    void deveLancarExcecaoQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(restaurantGateway.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.run(id));
    }

    @Test
    @DisplayName("Recusa id nulo")
    void deveRecusarIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
