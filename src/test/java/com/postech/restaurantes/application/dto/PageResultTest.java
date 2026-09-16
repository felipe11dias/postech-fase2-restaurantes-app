package com.postech.restaurantes.application.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PageResultTest {

    @Test
    @DisplayName("Calcula total de páginas arredondando para cima")
    void deveCalcularTotalDePaginas() {
        PageResult<String> result = new PageResult<>(List.of("a", "b"), 0, 2, 5);

        assertEquals(3, result.totalPages());
        assertTrue(result.hasNext());
        assertFalse(result.hasPrevious());
    }

    @Test
    @DisplayName("Última página não tem próxima e tem anterior")
    void deveIdentificarUltimaPagina() {
        PageResult<String> result = new PageResult<>(List.of("e"), 2, 2, 5);

        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());
    }

    @Test
    @DisplayName("Página vazia a partir do pedido")
    void deveCriarPaginaVazia() {
        PageResult<String> result = PageResult.empty(PageRequest.of(3, 7));

        assertTrue(result.content().isEmpty());
        assertEquals(3, result.page());
        assertEquals(7, result.size());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

    @Test
    @DisplayName("Mapeia o conteúdo preservando os metadados")
    void deveMapearConteudo() {
        PageResult<Integer> result = new PageResult<>(List.of(1, 2), 1, 2, 4);

        PageResult<String> mapped = result.map(String::valueOf);

        assertEquals(List.of("1", "2"), mapped.content());
        assertEquals(1, mapped.page());
        assertEquals(2, mapped.size());
        assertEquals(4, mapped.totalElements());
    }

    @Test
    @DisplayName("Conteúdo exposto é uma cópia imutável")
    void deveCopiarConteudo() {
        List<String> original = new ArrayList<>(List.of("a"));
        PageResult<String> result = new PageResult<>(original, 0, 1, 1);
        original.add("b");

        assertEquals(List.of("a"), result.content());
        assertThrows(UnsupportedOperationException.class, () -> result.content().add("c"));
    }

    @Test
    @DisplayName("Recusa conteúdo nulo, página negativa, tamanho zero ou total negativo")
    void deveRecusarMetadadosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(null, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of(), -1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of(), 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new PageResult<>(List.of(), 0, 1, -1));
    }
}
