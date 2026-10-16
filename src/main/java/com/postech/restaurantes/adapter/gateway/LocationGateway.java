package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.ILocationDataSource;
import com.postech.restaurantes.adapter.gateway.mapping.LocationMapping;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.ILocationGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.location.city.City;
import com.postech.restaurantes.domain.entity.location.state.State;
import java.util.List;
import java.util.UUID;

public final class LocationGateway implements ILocationGateway {
    private final ILocationDataSource dataSource;

    private LocationGateway(ILocationDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de localização inválida");
    }

    public static LocationGateway create(ILocationDataSource dataSource) {
        return new LocationGateway(dataSource);
    }

    @Override
    public List<State> findStates() {
        return dataSource.findStates().stream().map(LocationMapping::toEntity).toList();
    }

    @Override
    public PageResult<City> searchCities(UUID stateId, String name, PageRequest request) {
        return dataSource.searchCities(stateId, name, request).map(LocationMapping::toEntity);
    }
}
