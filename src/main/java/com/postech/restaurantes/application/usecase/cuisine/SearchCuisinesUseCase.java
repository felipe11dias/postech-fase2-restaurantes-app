package com.postech.restaurantes.application.usecase.cuisine;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import java.util.Set;

public final class SearchCuisinesUseCase {

    public static final String DEFAULT_SORT = "name";
    public static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "name", "createdAt", "lastUpdatedAt");

    private final ICuisineGateway cuisineGateway;

    private SearchCuisinesUseCase(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static SearchCuisinesUseCase create(ICuisineGateway cuisineGateway) {
        return new SearchCuisinesUseCase(cuisineGateway);
    }

    public PageResult<Cuisine> run(String name, PageRequest request) {
        Guard.requireNonNull(request, "Paginação inválida");
        return cuisineGateway.search(Guard.trimToNull(name), sanitizeSort(request));
    }

    private static PageRequest sanitizeSort(PageRequest request) {
        if (request.hasSort() && SORTABLE_PROPERTIES.contains(request.sortBy())) {
            return request;
        }
        return request.withSort(DEFAULT_SORT, SortDirection.ASC);
    }
}