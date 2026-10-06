package com.postech.restaurantes.application.usecase.restaurant;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.address;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FindRestaurantOwnerUseCaseTest {

    private final IRestaurantGateway restaurantGateway = mock(IRestaurantGateway.class);
    private final FindRestaurantOwnerUseCase useCase = FindRestaurantOwnerUseCase.create(restaurantGateway);

    @Test
    @DisplayName("Devolve o dono do restaurante")
    void deveDevolverODono() {
        UUID id = UUID.randomUUID();
        UUID dono = UUID.randomUUID();
        when(restaurantGateway.findById(id)).thenReturn(Optional.of(Restaurant.restore(id, dono, address(), "Sabor",
                LocalTime.of(8, 0), LocalTime.of(22, 0), null, null)));

        assertEquals(Optional.of(dono), useCase.run(id));
    }

    @Test
    @DisplayName("Restaurante inexistente não tem dono; id nulo é recusado")
    void deveDevolverVazioQuandoNaoExiste() {
        UUID id = UUID.randomUUID();
        when(restaurantGateway.findById(id)).thenReturn(Optional.empty());

        assertTrue(useCase.run(id).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> useCase.run(null));
    }
}
