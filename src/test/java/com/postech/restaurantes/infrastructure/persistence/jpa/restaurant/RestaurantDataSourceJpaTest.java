package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class RestaurantDataSourceJpaTest {

    private SpringDataRestaurantRepository repository;
    private RestaurantDataSourceJpa dataSource;

    private UUID id;
    private UUID userId;
    private UUID addressId;
    private RestaurantJpaEntity entity;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataRestaurantRepository.class);
        dataSource = new RestaurantDataSourceJpa(repository);

        id = UUID.randomUUID();
        userId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        entity = new RestaurantJpaEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setAddressId(addressId);
        entity.setName("Sabor");
        entity.setOfficeHourStart(LocalTime.of(8, 0));
        entity.setOfficeHourEnd(LocalTime.of(22, 0));
    }

    @Test
    @DisplayName("FindById com sucesso")
    void deveBuscarPorId() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        Optional<RestaurantData> result = dataSource.findById(id);

        assertTrue(result.isPresent());
        assertEquals("Sabor", result.get().name());
    }

    @Test
    @DisplayName("Busca paginada")
    void deveBuscarPaginado() {
        PageRequest request = PageRequest.of(0, 10).withSort("name", SortDirection.ASC);
        when(repository.findIdsByName(eq("sabor"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(id)));
        when(repository.findByIdIn(eq(List.of(id)), any())).thenReturn(List.of(entity));

        PageResult<RestaurantData> result = dataSource.search("sabor", request);

        assertEquals(1, result.totalElements());
    }

    @Test
    @DisplayName("Busca paginada quando resultado e vazio")
    void deveBuscarPaginadoQuandoVazio() {
        PageRequest request = PageRequest.of(0, 10);
        when(repository.findIdsByName(eq(""), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResult<RestaurantData> result = dataSource.search(null, request);

        assertEquals(0, result.totalElements());
    }

    @Test
    @DisplayName("Insercao e atualizacao")
    void deveInserirEAtualizar() {
        when(repository.saveAndFlush(any())).thenReturn(entity);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        RestaurantData data = new RestaurantData(id, userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);

        RestaurantData inserted = dataSource.insert(data);
        RestaurantData updated = dataSource.update(data);

        assertNotNull(inserted);
        assertNotNull(updated);
    }

    @Test
    @DisplayName("Lanca excecao ao atualizar inexistente")
    void deveLancarExcecaoAoAtualizarInexistente() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        RestaurantData data = new RestaurantData(id, userId, addressId, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);

        assertThrows(IllegalStateException.class, () -> dataSource.update(data));
    }

    @Test
    @DisplayName("Delecao")
    void deveDeletar() {
        dataSource.delete(id);

        verify(repository).deleteById(id);
    }
}
