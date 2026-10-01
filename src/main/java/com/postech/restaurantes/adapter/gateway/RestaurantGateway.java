package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.util.Optional;
import java.util.UUID;

/**
 * Tradutor entre o agregado {@link Restaurant} e o {@link RestaurantData} que a origem de dados entende.
 */
public final class RestaurantGateway implements IRestaurantGateway {

    private final IRestaurantDataSource dataSource;

    private RestaurantGateway(IRestaurantDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de restaurante inválida");
    }

    public static RestaurantGateway create(IRestaurantDataSource dataSource) {
        return new RestaurantGateway(dataSource);
    }

    @Override
    public Optional<Restaurant> findById(UUID id) {
        return dataSource.findById(id).map(RestaurantGateway::toEntity);
    }

    @Override
    public PageResult<Restaurant> search(String name, PageRequest request) {
        return dataSource.search(name, request).map(RestaurantGateway::toEntity);
    }

    @Override
    public Restaurant insert(Restaurant restaurant) {
        return toEntity(dataSource.insert(toData(restaurant)));
    }

    @Override
    public Restaurant update(Restaurant restaurant) {
        return toEntity(dataSource.update(toData(restaurant)));
    }

    @Override
    public void delete(UUID id) {
        dataSource.delete(id);
    }

    static Restaurant toEntity(RestaurantData data) {
        return Restaurant.restore(
                data.id(),
                data.userId(),
                data.addressId(),
                data.name(),
                data.officeHourStart(),
                data.officeHourEnd(),
                data.createdAt(),
                data.lastUpdatedAt()
        );
    }

    static RestaurantData toData(Restaurant restaurant) {
        return new RestaurantData(
                restaurant.getId(),
                restaurant.getUserId(),
                restaurant.getAddressId(),
                restaurant.getName(),
                restaurant.getOfficeHourStart(),
                restaurant.getOfficeHourEnd(),
                restaurant.getCreatedAt(),
                restaurant.getLastUpdatedAt()
        );
    }
}
