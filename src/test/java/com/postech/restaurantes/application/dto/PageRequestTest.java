package com.postech.restaurantes.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageRequestTest {

    @Test
    @DisplayName("Cria pedido sem ordenação com direção ASC por padrão")
    void deveCriarSemOrdenacao() {
        PageRequest request = PageRequest.of(2, 10);

        assertEquals(2, request.page());
        assertEquals(10, request.size());
        assertNull(request.sortBy());
        assertEquals(SortDirection.ASC, request.direction());
        assertFalse(request.hasSort());
        assertEquals(20, request.offset());
    }

    @Test
    @DisplayName("Primeira página usa o tamanho padrão")
    void deveUsarTamanhoPadraoNaPrimeiraPagina() {
        PageRequest request = PageRequest.first();

        assertEquals(0, request.page());
        assertEquals(PageRequest.DEFAULT_SIZE, request.size());
    }

    @Test
    @DisplayName("Aceita ordenação, aparando a propriedade")
    void deveAceitarOrdenacao() {
        PageRequest request = PageRequest.of(0, 5).withSort(" name ", SortDirection.DESC);

        assertTrue(request.hasSort());
        assertEquals("name", request.sortBy());
        assertEquals(SortDirection.DESC, request.direction());
    }

    @Test
    @DisplayName("Propriedade em branco e direção nula viram ausência e ASC")
    void deveNormalizarOrdenacaoAusente() {
        PageRequest request = new PageRequest(0, 5, "  ", null);

        assertFalse(request.hasSort());
        assertEquals(SortDirection.ASC, request.direction());
    }

    @Test
    @DisplayName("Recusa página negativa")
    void deveRecusarPaginaNegativa() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(-1, 10));
    }

    @Test
    @DisplayName("Recusa tamanho menor que 1 ou maior que o máximo")
    void deveRecusarTamanhoForaDoIntervalo() {
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, 0));
        assertThrows(IllegalArgumentException.class, () -> PageRequest.of(0, PageRequest.MAX_SIZE + 1));
    }
}
