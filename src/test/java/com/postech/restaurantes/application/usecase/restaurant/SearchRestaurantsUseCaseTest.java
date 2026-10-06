package com.postech.restaurantes.application.usecase.restaurant;

import static com.postech.restaurantes.application.usecase.UseCaseFixtures.address;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchRestaurantsUseCaseTest {

    private IRestaurantGateway restaurantGateway;
    private SearchRestaurantsUseCase useCase;

    @BeforeEach
    void setUp() {
        restaurantGateway = mock(IRestaurantGateway.class);
        useCase = SearchRestaurantsUseCase.create(restaurantGateway);
    }

    @Test
    @DisplayName("Executa busca paginada com ordenação válida")
    void deveExecutarBuscaPaginada() {
        PageRequest request = new PageRequest(0, 10, "name", SortDirection.ASC);
        Restaurant r = Restaurant.restore(UUID.randomUUID(), UUID.randomUUID(), address(), "Sabor",
                LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);
        PageResult<Restaurant> page = new PageResult<>(List.of(r), 0, 10, 1);
        when(restaurantGateway.search(eq("sabor"), eq(null), any())).thenReturn(page);

        PageResult<Restaurant> result = useCase.run("  sabor  ", null, request);

        assertEquals(1, result.totalElements());
    }

    @Test
    @DisplayName("Aplica ordenação padrão quando propriedade é inválida")
    void deveSanitizarOrdenacaoInvalida() {
        PageRequest request = new PageRequest(0, 10, "invalida", SortDirection.ASC);
        PageResult<Restaurant> page = new PageResult<>(List.of(), 0, 10, 0);
        when(restaurantGateway.search(any(), any(), any())).thenReturn(page);

        PageResult<Restaurant> result = useCase.run(null, null, request);

        assertEquals(0, result.totalElements());
    }

    @Test
    @DisplayName("Recusa requisição de página nula")
    void deveRecusarRequisicaoNula() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run("sabor", null, null));
    }

    @Test
    @DisplayName("Repassa o dono para a busca só dos restaurantes dele")
    void deveFiltrarPeloDono() {
        UUID dono = UUID.randomUUID();
        PageRequest request = new PageRequest(0, 10, "name", SortDirection.ASC);
        when(restaurantGateway.search(null, dono, request)).thenReturn(new PageResult<>(List.of(), 0, 10, 0));

        useCase.run(null, dono, request);

        verify(restaurantGateway).search(null, dono, request);
    }
}
