package com.postech.restaurantes.infrastructure.persistence.jpa.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
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

class CuisineDataSourceJpaTest {

    private SpringDataCuisineRepository repository;
    private CuisineDataSourceJpa dataSource;
    private UUID id;
    private CuisineJpaEntity entity;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataCuisineRepository.class);
        dataSource = new CuisineDataSourceJpa(repository);
        id = UUID.randomUUID();
        entity = new CuisineJpaEntity();
        entity.setId(id);
        entity.setName("Italiana");
    }

    @Test
    @DisplayName("Busca por id converte a entidade JPA no registro")
    void deveBuscarPorId() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        CuisineData data = dataSource.findById(id).orElseThrow();

        assertEquals(id, data.id());
        assertEquals("Italiana", data.name());
    }

    @Test
    @DisplayName("Busca por nome usa a consulta que não diferencia maiúsculas")
    void deveBuscarPorNome() {
        when(repository.findByNameIgnoreCase("italiana")).thenReturn(Optional.of(entity));

        assertEquals(id, dataSource.findByName("italiana").orElseThrow().id());
    }

    @Test
    @DisplayName("Busca por vários ids converte as entidades encontradas")
    void deveBuscarVariosPorId() {
        when(repository.findAllById(List.of(id))).thenReturn(List.of(entity));

        List<CuisineData> encontrados = dataSource.findAllById(List.of(id));

        assertEquals(1, encontrados.size());
        assertEquals("Italiana", encontrados.get(0).name());
    }

    @Test
    @DisplayName("Listagem pede ao repositório a página e a ordenação pedidas e converte o resultado")
    void deveListarComAOrdenacaoPedida() {
        when(repository.findByNameContainingIgnoreCase(eq("ital"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        PageResult<CuisineData> pagina = dataSource.search("ital", PageRequest.of(1, 10).withSort("name", SortDirection.DESC));

        assertEquals(1, pagina.page());
        assertEquals("Italiana", pagina.content().get(0).name());
        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByNameContainingIgnoreCase(eq("ital"), pedido.capture());
        assertEquals(1, pedido.getValue().getPageNumber());
        assertEquals(Sort.Direction.DESC, pedido.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    @DisplayName("Listagem sem nome busca todos, na ordem padrão: nome crescente")
    void deveListarSemFiltroNaOrdemPadrao() {
        when(repository.findByNameContainingIgnoreCase(eq(""), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResult<CuisineData> pagina = dataSource.search(null, PageRequest.of(0, 10));

        assertEquals(0, pagina.totalElements());
        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByNameContainingIgnoreCase(eq(""), pedido.capture());
        assertEquals(Sort.Direction.ASC, pedido.getValue().getSort().getOrderFor("name").getDirection());
    }

    @Test
    @DisplayName("Ordenação por campo fora do mapa cai no padrão: nome")
    void deveUsarOrdenacaoPadraoParaCampoDesconhecido() {
        when(repository.findByNameContainingIgnoreCase(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        dataSource.search("x", PageRequest.of(0, 10).withSort("senha", SortDirection.ASC));

        ArgumentCaptor<Pageable> pedido = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByNameContainingIgnoreCase(any(), pedido.capture());
        assertEquals("name", pedido.getValue().getSort().iterator().next().getProperty());
    }

    @Test
    @DisplayName("Gravação cria uma entidade nova, sem id, e devolve o registro gravado")
    void deveInserir() {
        when(repository.saveAndFlush(any())).thenAnswer(chamada -> chamada.getArgument(0));

        CuisineData gravado = dataSource.insert(new CuisineData(null, "Japonesa", null, null));

        ArgumentCaptor<CuisineJpaEntity> enviado = ArgumentCaptor.forClass(CuisineJpaEntity.class);
        verify(repository).saveAndFlush(enviado.capture());
        assertNull(enviado.getValue().getId());
        assertEquals("Japonesa", gravado.name());
    }

    @Test
    @DisplayName("Alteração troca o nome na mesma linha do banco")
    void deveAtualizar() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(chamada -> chamada.getArgument(0));

        CuisineData atualizado = dataSource.update(new CuisineData(id, "Massas", null, null));

        assertEquals(id, atualizado.id());
        assertEquals("Massas", atualizado.name());
        assertEquals("Massas", entity.getName());
    }

    @Test
    @DisplayName("Alterar um tipo que não existe no banco é erro de estado")
    void deveLancarErroAoAtualizarInexistente() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> dataSource.update(new CuisineData(id, "Massas", null, null)));
    }

    @Test
    @DisplayName("Pergunta ao repositório se algum restaurante usa o tipo")
    void deveDizerSeEstaEmUso() {
        when(repository.existsInRestaurants(id)).thenReturn(true);

        assertTrue(dataSource.existsInRestaurants(id));
    }

    @Test
    @DisplayName("Exclusão é repassada ao repositório")
    void deveExcluir() {
        dataSource.delete(id);

        verify(repository).deleteById(id);
    }
}
