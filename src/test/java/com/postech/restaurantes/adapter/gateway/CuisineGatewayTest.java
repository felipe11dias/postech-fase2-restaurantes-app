package com.postech.restaurantes.adapter.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.ICuisineDataSource;
import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CuisineGatewayTest {

    private ICuisineDataSource dataSource;
    private CuisineGateway gateway;
    private UUID id;
    private CuisineData data;

    @BeforeEach
    void setUp() {
        dataSource = mock(ICuisineDataSource.class);
        gateway = CuisineGateway.create(dataSource);
        id = UUID.randomUUID();
        data = new CuisineData(id, "Italiana", LocalDateTime.of(2026, 1, 1, 10, 0), LocalDateTime.of(2026, 1, 2, 10, 0));
    }

    @Test
    @DisplayName("Busca por id converte o registro em entidade, mantendo id e datas")
    void deveBuscarPorId() {
        when(dataSource.findById(id)).thenReturn(Optional.of(data));

        Cuisine cuisine = gateway.findById(id).orElseThrow();

        assertEquals(id, cuisine.getId());
        assertEquals("Italiana", cuisine.getName());
        assertEquals(data.createdAt(), cuisine.getCreatedAt());
        assertEquals(data.lastUpdatedAt(), cuisine.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Busca por nome converte o registro em entidade; sem resultado, devolve vazio")
    void deveBuscarPorNome() {
        when(dataSource.findByName("Italiana")).thenReturn(Optional.of(data));
        when(dataSource.findByName("Outra")).thenReturn(Optional.empty());

        assertEquals(id, gateway.findByName("Italiana").orElseThrow().getId());
        assertTrue(gateway.findByName("Outra").isEmpty());
    }

    @Test
    @DisplayName("Busca por vários ids converte cada registro encontrado")
    void deveBuscarVariosPorId() {
        when(dataSource.findAllById(List.of(id))).thenReturn(List.of(data));

        List<Cuisine> encontrados = gateway.findAllById(List.of(id));

        assertEquals(1, encontrados.size());
        assertEquals("Italiana", encontrados.get(0).getName());
    }

    @Test
    @DisplayName("Busca paginada converte cada registro da página e mantém o total")
    void deveBuscarPaginado() {
        PageRequest pedido = PageRequest.of(0, 10);
        when(dataSource.search("ital", pedido)).thenReturn(new PageResult<>(List.of(data), 0, 10, 1));

        PageResult<Cuisine> pagina = gateway.search("ital", pedido);

        assertEquals(1, pagina.totalElements());
        assertEquals("Italiana", pagina.content().get(0).getName());
    }

    @Test
    @DisplayName("Inserção entrega à origem de dados um registro sem id e devolve a entidade gravada")
    void deveInserir() {
        when(dataSource.insert(any())).thenReturn(data);

        Cuisine gravado = gateway.insert(Cuisine.create("Italiana"));

        ArgumentCaptor<CuisineData> enviado = ArgumentCaptor.forClass(CuisineData.class);
        verify(dataSource).insert(enviado.capture());
        assertNull(enviado.getValue().id());
        assertEquals("Italiana", enviado.getValue().name());
        assertEquals(id, gravado.getId());
    }

    @Test
    @DisplayName("Atualização entrega à origem de dados o registro com o id e devolve a entidade")
    void deveAtualizar() {
        when(dataSource.update(any())).thenReturn(data);

        Cuisine atualizado = gateway.update(Cuisine.restore(id, "Italiana", null, null));

        ArgumentCaptor<CuisineData> enviado = ArgumentCaptor.forClass(CuisineData.class);
        verify(dataSource).update(enviado.capture());
        assertEquals(id, enviado.getValue().id());
        assertEquals(id, atualizado.getId());
    }

    @Test
    @DisplayName("Pergunta à origem de dados se algum restaurante usa o tipo")
    void deveDizerSeEstaEmUso() {
        UUID usado = UUID.randomUUID();
        when(dataSource.existsInRestaurants(usado)).thenReturn(true);

        assertTrue(gateway.isUsedByRestaurants(usado));
        assertFalse(gateway.isUsedByRestaurants(UUID.randomUUID()));
    }

    @Test
    @DisplayName("Exclusão é repassada à origem de dados")
    void deveExcluir() {
        gateway.delete(id);

        verify(dataSource).delete(id);
    }

    @Test
    @DisplayName("Recusa origem de dados nula")
    void deveRecusarOrigemDeDadosNula() {
        assertThrows(IllegalArgumentException.class, () -> CuisineGateway.create(null));
    }
}
