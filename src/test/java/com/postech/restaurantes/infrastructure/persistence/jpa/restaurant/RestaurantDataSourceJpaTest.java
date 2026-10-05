package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_DATA;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.addressEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
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
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class RestaurantDataSourceJpaTest {

    private SpringDataRestaurantRepository repository;
    private RestaurantDataSourceJpa dataSource;

    private UUID id;
    private UUID userId;
    private RestaurantJpaEntity entity;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataRestaurantRepository.class);
        dataSource = new RestaurantDataSourceJpa(repository);

        id = UUID.randomUUID();
        userId = UUID.randomUUID();

        entity = new RestaurantJpaEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setAddress(addressEntity());
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
    @DisplayName("Busca paginada em ordem decrescente repassa a direção ao banco")
    void deveBuscarPaginadoEmOrdemDecrescente() {
        PageRequest request = PageRequest.of(0, 10).withSort("name", SortDirection.DESC);
        when(repository.findIdsByName(eq("sabor"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(id)));
        when(repository.findByIdIn(eq(List.of(id)), any())).thenReturn(List.of(entity));

        dataSource.search("sabor", request);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findIdsByName(eq("sabor"), captor.capture());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("name").getDirection());
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
    @DisplayName("Inserção grava o restaurante com uma linha de endereço nova, sem id")
    void deveInserirComEnderecoNovo() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantData data = new RestaurantData(null, userId, ADDRESS_DATA, "Sabor", LocalTime.of(8, 0),
                LocalTime.of(22, 0), null, null);

        RestaurantData inserted = dataSource.insert(data);

        ArgumentCaptor<RestaurantJpaEntity> captor = ArgumentCaptor.forClass(RestaurantJpaEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getAddress().getId());
        assertEquals("Rua das Flores", inserted.address().street());
    }

    @Test
    @DisplayName("Atualização troca os campos do endereço na mesma linha, mantendo o id dela")
    void deveAtualizarOEnderecoNaMesmaLinha() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AddressData outro = new AddressData(UUID.randomUUID(), "Av. B", null, null, null, "Rio", "RJ", "20000000");
        RestaurantData data = new RestaurantData(id, userId, outro, "Novo", LocalTime.of(8, 0), LocalTime.of(22, 0),
                null, null);

        RestaurantData updated = dataSource.update(data);

        assertEquals(ADDRESS_ID, updated.address().id());
        assertEquals("Av. B", updated.address().street());
        assertEquals("Novo", updated.name());
    }

    @Test
    @DisplayName("Lanca excecao ao atualizar inexistente")
    void deveLancarExcecaoAoAtualizarInexistente() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        RestaurantData data = new RestaurantData(id, userId, ADDRESS_DATA, "Sabor", LocalTime.of(8, 0), LocalTime.of(22, 0), null, null);

        assertThrows(IllegalStateException.class, () -> dataSource.update(data));
    }

    @Test
    @DisplayName("Delecao")
    void deveDeletar() {
        dataSource.delete(id);

        verify(repository).deleteById(id);
    }
}
