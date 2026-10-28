package com.postech.restaurantes.application.usecase.cuisine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SearchCuisinesUseCaseTest {

    private ICuisineGateway cuisineGateway;
    private SearchCuisinesUseCase useCase;

    @BeforeEach
    void setUp() {
        cuisineGateway = mock(ICuisineGateway.class);
        useCase = SearchCuisinesUseCase.create(cuisineGateway);
        when(cuisineGateway.search(any(), any())).thenReturn(new PageResult<>(List.of(), 0, 10, 0));
    }

    @Test
    @DisplayName("Repassa o nome sem espaços e a ordenação permitida como vieram")
    void deveRepassarBuscaComOrdenacaoPermitida() {
        PageRequest pedido = new PageRequest(0, 10, "name", SortDirection.DESC);
        Cuisine italiana = Cuisine.restore(UUID.randomUUID(), "Italiana", null, null);
        when(cuisineGateway.search("ital", pedido)).thenReturn(new PageResult<>(List.of(italiana), 0, 10, 1));

        PageResult<Cuisine> pagina = useCase.run("  ital  ", pedido);

        assertEquals(1, pagina.totalElements());
        assertEquals("Italiana", pagina.content().get(0).getName());
    }

    @Test
    @DisplayName("Nome em branco vira busca sem filtro")
    void deveBuscarSemFiltroQuandoNomeEmBranco() {
        useCase.run("   ", PageRequest.of(0, 10));

        verify(cuisineGateway).search(isNull(), any());
    }

    @Test
    @DisplayName("Ordenação por campo não permitido cai na ordenação padrão: nome, crescente")
    void deveUsarOrdenacaoPadraoQuandoCampoNaoPermitido() {
        useCase.run(null, new PageRequest(0, 10, "senha", SortDirection.DESC));

        ArgumentCaptor<PageRequest> enviado = ArgumentCaptor.forClass(PageRequest.class);
        verify(cuisineGateway).search(any(), enviado.capture());
        assertEquals("name", enviado.getValue().sortBy());
        assertEquals(SortDirection.ASC, enviado.getValue().direction());
    }

    @Test
    @DisplayName("Sem ordenação pedida, usa a ordenação padrão")
    void deveUsarOrdenacaoPadraoQuandoNaoHaPedido() {
        useCase.run(null, PageRequest.of(0, 10));

        ArgumentCaptor<PageRequest> enviado = ArgumentCaptor.forClass(PageRequest.class);
        verify(cuisineGateway).search(any(), enviado.capture());
        assertEquals("name", enviado.getValue().sortBy());
    }

    @Test
    @DisplayName("Recusa pedido de página nulo")
    void deveRecusarPaginacaoNula() {
        assertThrows(IllegalArgumentException.class, () -> useCase.run("ital", null));
    }
}
