package com.postech.restaurantes.application.usecase.location;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.application.gateway.ILocationGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.location.city.City;
import java.util.UUID;

/** Consulta cidades existentes por estado e nome, sempre em ordem alfabética. */
public final class SearchCitiesUseCase {
    private final ILocationGateway gateway;

    private SearchCitiesUseCase(ILocationGateway gateway) { this.gateway = gateway; }

    public static SearchCitiesUseCase create(ILocationGateway gateway) {
        return new SearchCitiesUseCase(gateway);
    }

    public PageResult<City> run(UUID stateId, String name, PageRequest request) {
        Guard.requireNonNull(request, "Paginação inválida");
        return gateway.searchCities(stateId, Guard.trimToNull(name), request.withSort("name", SortDirection.ASC));
    }
}
