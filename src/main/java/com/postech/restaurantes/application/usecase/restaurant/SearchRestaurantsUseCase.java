package com.postech.restaurantes.application.usecase.restaurant;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.util.Set;
import java.util.UUID;

/**
 * Listagem paginada com busca parcial por nome de restaurante e, opcionalmente, só os de um dono.
 */
public final class SearchRestaurantsUseCase {

    public static final String DEFAULT_SORT = "name";
    public static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "name", "createdAt", "lastUpdatedAt");

    private final IRestaurantGateway restaurantGateway;

    private SearchRestaurantsUseCase(IRestaurantGateway restaurantGateway) {
        this.restaurantGateway = restaurantGateway;
    }

    public static SearchRestaurantsUseCase create(IRestaurantGateway restaurantGateway) {
        return new SearchRestaurantsUseCase(restaurantGateway);
    }

    public PageResult<Restaurant> run(String name, UUID ownerId, PageRequest request) {
        Guard.requireNonNull(request, "Paginação inválida");
        return restaurantGateway.search(Guard.trimToNull(name), ownerId, sanitizeSort(request));
    }

    private static PageRequest sanitizeSort(PageRequest request) {
        if (request.hasSort() && SORTABLE_PROPERTIES.contains(request.sortBy())) {
            return request;
        }
        return request.withSort(DEFAULT_SORT, SortDirection.ASC);
    }
}
