package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.ICuisineDataSource;
import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class CuisineGateway implements ICuisineGateway {

    private final ICuisineDataSource dataSource;

    private CuisineGateway(ICuisineDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de tipo de cozinha inválida");
    }

    public static CuisineGateway create(ICuisineDataSource dataSource) {
        return new CuisineGateway(dataSource);
    }

    @Override
    public Optional<Cuisine> findById(UUID id) {
        return dataSource.findById(id).map(CuisineGateway::toEntity);
    }

    @Override
    public List<Cuisine> findAllById(Collection<UUID> ids) {
        return dataSource.findAllById(ids).stream().map(CuisineGateway::toEntity).toList();
    }

    @Override
    public Optional<Cuisine> findByName(String name) {
        return dataSource.findByName(name).map(CuisineGateway::toEntity);
    }

    @Override
    public PageResult<Cuisine> search(String name, PageRequest request) {
        return dataSource.search(name, request).map(CuisineGateway::toEntity);
    }

    @Override
    public Cuisine insert(Cuisine cuisine) {
        return toEntity(dataSource.insert(toData(cuisine)));
    }

    @Override
    public Cuisine update(Cuisine cuisine) {
        return toEntity(dataSource.update(toData(cuisine)));
    }

    @Override
    public boolean isUsedByRestaurants(UUID id) {
        return dataSource.existsInRestaurants(id);
    }

    @Override
    public void delete(UUID id) {
        dataSource.delete(id);
    }

    static Cuisine toEntity(CuisineData data) {
        return Cuisine.restore(data.id(), data.name(), data.createdAt(), data.lastUpdatedAt());
    }

    static CuisineData toData(Cuisine cuisine) {
        return new CuisineData(cuisine.getId(), cuisine.getName(), cuisine.getCreatedAt(),
                cuisine.getLastUpdatedAt());
    }
}