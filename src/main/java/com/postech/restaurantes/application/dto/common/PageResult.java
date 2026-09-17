package com.postech.restaurantes.application.dto.common;

import com.postech.restaurantes.domain.Guard;
import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados do núcleo, sem {@code Page} do Spring. Imutável; {@link #map} permite
 * ao presenter converter o conteúdo sem perder os metadados.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements) {

    public PageResult {
        Guard.requireNonNull(content, "Conteúdo da página inválido");
        Guard.require(page >= 0, "Página deve ser maior ou igual a zero");
        Guard.require(size >= 1, "Tamanho da página deve ser maior que zero");
        Guard.require(totalElements >= 0, "Total de elementos inválido");
        content = List.copyOf(content);
    }

    public static <T> PageResult<T> empty(PageRequest request) {
        return new PageResult<>(List.of(), request.page(), request.size(), 0);
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
