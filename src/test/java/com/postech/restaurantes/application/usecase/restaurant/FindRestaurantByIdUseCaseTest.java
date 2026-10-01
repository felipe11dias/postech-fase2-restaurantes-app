package com.postech.restaurantes.application.usecase.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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

class FindRestaurantByIdUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private FindRestaurantByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        useCase = FindRestaurantByIdUseCase.create(restaurantGateway);
    }

    @Test
    @DisplayName("Encontra restaurante quando id existe")
    void deveEncontrarQuandoIdExiste() {
        UUID id = UUID.randomUUID();
        Restaurant restaurant = Restaurant.restore(id, UUID.randomUUID(), UUID.randomUUID(), "Sabor",
                LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);
        when(restaurantGateway.findById(id)).thenReturn(Optional.of(restaurant));

        Restaurant result = useCase.run(id);

        assertEquals(id, result.getId());
    }

    @Test
    @DisplayName("Lança exceção quando id não é encontrado")
    void deveLancarExcecaoQuandoNaoEncontrado() {
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
