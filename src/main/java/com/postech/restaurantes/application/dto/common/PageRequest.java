package com.postech.restaurantes.application.dto.common;

import com.postech.restaurantes.domain.Guard;

/**
 * Pedido de página do núcleo, sem {@code Pageable} do Spring. Página base 0; tamanho entre 1
 * e {@value #MAX_SIZE}. A propriedade de ordenação é opcional — quem a interpreta (e valida
 * contra a lista permitida) é o caso de uso.
 */
public record PageRequest(int page, int size, String sortBy, SortDirection direction) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageRequest {
        Guard.require(page >= 0, "Página deve ser maior ou igual a zero");
        Guard.require(size >= 1 && size <= MAX_SIZE, "Tamanho da página deve estar entre 1 e " + MAX_SIZE);
        sortBy = Guard.trimToNull(sortBy);
        direction = direction == null ? SortDirection.ASC : direction;
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null, SortDirection.ASC);
    }

    public static PageRequest first() {
        return of(0, DEFAULT_SIZE);
    }

    public PageRequest withSort(String property, SortDirection newDirection) {
        return new PageRequest(page, size, property, newDirection);
    }

    public boolean hasSort() {
        return sortBy != null;
    }

    /** Deslocamento absoluto do primeiro item da página. */
    public long offset() {
        return (long) page * size;
    }
}
