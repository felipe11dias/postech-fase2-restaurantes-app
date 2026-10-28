package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import com.postech.restaurantes.LocationFixtures;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_DATA;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.OFFICE_HOURS_DATA;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.addressEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.adapter.datasource.data.OfficeHourData;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.persistence.jpa.cuisine.CuisineJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.cuisine.SpringDataCuisineRepository;
import com.postech.restaurantes.infrastructure.persistence.jpa.restaurant.officehour.OfficeHourJpaEntity;
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
    private SpringDataCuisineRepository cuisineRepository;
    private RestaurantDataSourceJpa dataSource;

    private UUID id;
    private UUID userId;
    private RestaurantJpaEntity entity;
    private OfficeHourJpaEntity segunda;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataRestaurantRepository.class);
        cuisineRepository = mock(SpringDataCuisineRepository.class);
        var resolver = mock(com.postech.restaurantes.infrastructure.persistence.jpa.location.CityReferenceResolver.class);
        when(resolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn(
                com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.cityEntity());
        dataSource = new RestaurantDataSourceJpa(repository, resolver, cuisineRepository);

        id = UUID.randomUUID();
        userId = UUID.randomUUID();

        entity = new RestaurantJpaEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setAddress(addressEntity());
        entity.setName("Sabor");
        segunda = new OfficeHourJpaEntity();
        segunda.setId(UUID.randomUUID());
        segunda.setDayOfWeek("MONDAY");
        segunda.setStartTime(LocalTime.of(8, 0));
        segunda.setEndTime(LocalTime.of(22, 0));
        entity.replaceOfficeHours(List.of(segunda));
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

        PageResult<RestaurantData> result = dataSource.search("sabor", null, null, request);

        assertEquals(1, result.totalElements());
    }

    @Test
    @DisplayName("Busca paginada em ordem decrescente repassa a direção ao banco")
    void deveBuscarPaginadoEmOrdemDecrescente() {
        PageRequest request = PageRequest.of(0, 10).withSort("name", SortDirection.DESC);
        when(repository.findIdsByName(eq("sabor"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(id)));
        when(repository.findByIdIn(eq(List.of(id)), any())).thenReturn(List.of(entity));

        dataSource.search("sabor", null, null, request);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findIdsByName(eq("sabor"), captor.capture());
        assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    @DisplayName("Com dono, a busca pagina só os ids dos restaurantes dele")
    void deveBuscarSoOsRestaurantesDoDono() {
        PageRequest request = PageRequest.of(0, 10);
        when(repository.findIdsByNameAndUserId(eq(""), eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(id)));
        when(repository.findByIdIn(eq(List.of(id)), any())).thenReturn(List.of(entity));

        PageResult<RestaurantData> result = dataSource.search(null, userId, null, request);

        assertEquals(1, result.totalElements());
        verify(repository, never()).findIdsByName(any(), any());
    }

    @Test
    @DisplayName("Com tipo de cozinha, a busca pagina só os ids dos restaurantes que o servem")
    void deveBuscarSoOsRestaurantesDoTipoDeCozinha() {
        PageRequest request = PageRequest.of(0, 10);
        UUID cozinha = UUID.randomUUID();
        when(repository.findIdsByNameAndCuisineId(eq(""), eq(cozinha), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(id)));
        when(repository.findByIdIn(eq(List.of(id)), any())).thenReturn(List.of(entity));

        PageResult<RestaurantData> result = dataSource.search(null, null, cozinha, request);

        assertEquals(1, result.totalElements());
        verify(repository, never()).findIdsByName(any(), any());
    }

    @Test
    @DisplayName("Com dono e tipo de cozinha, a busca combina os dois filtros")
    void deveBuscarPeloDonoEPeloTipoDeCozinha() {
        PageRequest request = PageRequest.of(0, 10);
        UUID cozinha = UUID.randomUUID();
        when(repository.findIdsByNameAndUserIdAndCuisineId(eq("sabor"), eq(userId), eq(cozinha), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(id)));
        when(repository.findByIdIn(eq(List.of(id)), any())).thenReturn(List.of(entity));

        PageResult<RestaurantData> result = dataSource.search("sabor", userId, cozinha, request);

        assertEquals(1, result.totalElements());
        verify(repository, never()).findIdsByNameAndUserId(any(), any(), any());
    }

    @Test
    @DisplayName("Busca paginada quando resultado e vazio")
    void deveBuscarPaginadoQuandoVazio() {
        PageRequest request = PageRequest.of(0, 10);
        when(repository.findIdsByName(eq(""), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResult<RestaurantData> result = dataSource.search(null, null, null, request);

        assertEquals(0, result.totalElements());
    }

    @Test
    @DisplayName("Inserção grava o restaurante com uma linha de endereço nova, sem id")
    void deveInserirComEnderecoNovo() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantData data = new RestaurantData(null, userId, ADDRESS_DATA, "Sabor", OFFICE_HOURS_DATA, List.of(), null, null);

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
        AddressData outro = new AddressData(UUID.randomUUID(), LocationFixtures.cityId("Rio", "RJ"), "Av. B", null, null, null, "20000000", LocationFixtures.cityData("Rio", "RJ"));
        RestaurantData data = new RestaurantData(id, userId, outro, "Novo", OFFICE_HOURS_DATA, List.of(), null, null);

        RestaurantData updated = dataSource.update(data);

        assertEquals(ADDRESS_ID, updated.address().id());
        assertEquals("Av. B", updated.address().street());
        assertEquals("Novo", updated.name());
    }

    @Test
    @DisplayName("Atualização mantém na mesma linha o horário que continua (dia e abertura), cria o novo e tira o ausente")
    void deveReconciliarOsHorarios() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantData data = new RestaurantData(id, userId, ADDRESS_DATA, "Sabor", List.of(
                new OfficeHourData("MONDAY", LocalTime.of(8, 0), LocalTime.of(23, 0)),
                new OfficeHourData("TUESDAY", LocalTime.of(10, 0), LocalTime.of(14, 0))), List.of(), null, null);

        RestaurantData updated = dataSource.update(data);

        assertEquals(2, entity.getOfficeHours().size());
        assertSame(segunda, entity.getOfficeHours().get(0));
        assertEquals(LocalTime.of(23, 0), segunda.getEndTime());
        assertNull(entity.getOfficeHours().get(1).getId());
        assertEquals(new OfficeHourData("TUESDAY", LocalTime.of(10, 0), LocalTime.of(14, 0)), updated.officeHours().get(1));
    }

    @Test
    @DisplayName("Os tipos de cozinha do restaurante saem no registro, vindos da ligação com o catálogo")
    void deveTraduzirOsTiposDeCozinha() {
        CuisineJpaEntity italiana = cuisineEntity("Italiana");
        entity.replaceCuisines(List.of(italiana));
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        RestaurantData result = dataSource.findById(id).orElseThrow();

        assertEquals(List.of(new CuisineData(italiana.getId(), "Italiana", null, null)), result.cuisines());
    }

    @Test
    @DisplayName("Gravar liga o restaurante às linhas do catálogo que já existem, pelos ids, sem copiá-las")
    void deveLigarOsTiposDeCozinhaDoCatalogo() {
        CuisineJpaEntity italiana = cuisineEntity("Italiana");
        when(cuisineRepository.findAllById(List.of(italiana.getId()))).thenReturn(List.of(italiana));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantData data = new RestaurantData(null, userId, ADDRESS_DATA, "Sabor", OFFICE_HOURS_DATA,
                List.of(new CuisineData(italiana.getId(), "Italiana", null, null)), null, null);

        RestaurantData inserted = dataSource.insert(data);

        ArgumentCaptor<RestaurantJpaEntity> captor = ArgumentCaptor.forClass(RestaurantJpaEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertSame(italiana, captor.getValue().getCuisines().iterator().next());
        assertEquals("Italiana", inserted.cuisines().get(0).name());
    }

    @Test
    @DisplayName("Atualizar troca a lista de ligações: as que saíram, saem; as novas, entram")
    void deveTrocarOsTiposDeCozinhaNaAtualizacao() {
        CuisineJpaEntity italiana = cuisineEntity("Italiana");
        CuisineJpaEntity japonesa = cuisineEntity("Japonesa");
        entity.replaceCuisines(List.of(italiana));
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(cuisineRepository.findAllById(List.of(japonesa.getId()))).thenReturn(List.of(japonesa));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantData data = new RestaurantData(id, userId, ADDRESS_DATA, "Sabor", OFFICE_HOURS_DATA,
                List.of(new CuisineData(japonesa.getId(), "Japonesa", null, null)), null, null);

        dataSource.update(data);

        assertEquals(1, entity.getCuisines().size());
        assertSame(japonesa, entity.getCuisines().iterator().next());
    }

    private static CuisineJpaEntity cuisineEntity(String name) {
        CuisineJpaEntity cuisine = new CuisineJpaEntity();
        cuisine.setId(UUID.randomUUID());
        cuisine.setName(name);
        return cuisine;
    }

    @Test
    @DisplayName("Lanca excecao ao atualizar inexistente")
    void deveLancarExcecaoAoAtualizarInexistente() {
        when(repository.findById(id)).thenReturn(Optional.empty());
        RestaurantData data = new RestaurantData(id, userId, ADDRESS_DATA, "Sabor", OFFICE_HOURS_DATA, List.of(), null, null);

        assertThrows(IllegalStateException.class, () -> dataSource.update(data));
    }

    @Test
    @DisplayName("Diz se o usuário tem restaurante pelo repositório")
    void deveDizerSeOUsuarioTemRestaurante() {
        when(repository.existsByUserId(userId)).thenReturn(true);

        assertTrue(dataSource.existsByUserId(userId));
        verify(repository).existsByUserId(userId);
    }

    @Test
    @DisplayName("Exclusão dos restaurantes do usuário passa pelas entidades, para a cascata levar o endereço")
    void deveExcluirOsRestaurantesDoUsuario() {
        when(repository.findByUserId(userId)).thenReturn(List.of(entity));

        dataSource.deleteByUserId(userId);

        verify(repository).deleteAll(List.of(entity));
    }

    @Test
    @DisplayName("Delecao")
    void deveDeletar() {
        dataSource.delete(id);

        verify(repository).deleteById(id);
    }
}
