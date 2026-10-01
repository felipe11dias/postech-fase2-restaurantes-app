package com.postech.restaurantes.adapter.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RestaurantGatewayTest {

    private IRestaurantDataSource dataSource;
    private RestaurantGateway gateway;

    private UUID id;
    private UUID userId;
    private UUID addressId;
    private RestaurantData data;

    @BeforeEach
    void setUp() {
        dataSource = mock(IRestaurantDataSource.class);
        gateway = RestaurantGateway.create(dataSource);

        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        data = new RestaurantData(id, userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("Busca por ID com sucesso")
    void deveBuscarPorId() {
        when(dataSource.findById(id)).thenReturn(Optional.of(data));

        Optional<Restaurant> result = gateway.findById(id);

        assertTrue(result.isPresent());
        assertEquals("Sabor", result.get().getName());
    }

    @Test
    @DisplayName("Busca paginada")
    void deveBuscarPaginado() {
        PageRequest req = PageRequest.of(0, 10);
        PageResult<RestaurantData> page = new PageResult<>(List.of(data), 0, 10, 1);
        when(dataSource.search("sabor", req)).thenReturn(page);

        PageResult<Restaurant> result = gateway.search("sabor", req);

        assertEquals(1, result.totalElements());
    }

    @Test
    @DisplayName("Inserção e atualização")
    void deveInserirEAtualizar() {
        when(dataSource.insert(any())).thenReturn(data);
        when(dataSource.update(any())).thenReturn(data);

        Restaurant r = Restaurant.restore(id, userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);

        Restaurant inserted = gateway.insert(r);
        Restaurant updated = gateway.update(r);

        assertNotNull(inserted);
        assertNotNull(updated);
    }

    @Test
    @DisplayName("Deleção")
    void deveDeletar() {
        gateway.delete(id);

        verify(dataSource).delete(id);
    }

    @Test
    @DisplayName("Recusa datasource nulo")
    void deveRecusarDatasourceNulo() {
        assertThrows(IllegalArgumentException.class, () -> RestaurantGateway.create(null));
    }
}
