package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.data.OfficeHourData;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.adapter.gateway.mapping.AddressMapping;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.restaurant.OfficeHour;
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
    public PageResult<Restaurant> search(String name, UUID ownerId, PageRequest request) {
        return dataSource.search(name, ownerId, request).map(RestaurantGateway::toEntity);
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
    public boolean existsByUserId(UUID userId) {
        return dataSource.existsByUserId(userId);
    }

    @Override
    public void delete(UUID id) {
        dataSource.delete(id);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        dataSource.deleteByUserId(userId);
    }

    static Restaurant toEntity(RestaurantData data) {
        return Restaurant.restore(
                data.id(),
                data.userId(),
                AddressMapping.toEntity(data.address()),
                data.name(),
                data.officeHours().stream().map(RestaurantGateway::toEntity).toList(),
                data.createdAt(),
                data.lastUpdatedAt()
        );
    }

    static RestaurantData toData(Restaurant restaurant) {
        return new RestaurantData(
                restaurant.getId(),
                restaurant.getUserId(),
                AddressMapping.toData(restaurant.getAddress()),
                restaurant.getName(),
                restaurant.getOfficeHours().stream().map(RestaurantGateway::toData).toList(),
                restaurant.getCreatedAt(),
                restaurant.getLastUpdatedAt()
        );
    }

    /** O dia volta pelo nome, e passa pelo domínio: valor desconhecido na origem é recusado com a mensagem dele. */
    private static OfficeHour toEntity(OfficeHourData data) {
        return new OfficeHour(OfficeHour.dayOf(data.dayOfWeek()), data.startTime(), data.endTime());
    }

    private static OfficeHourData toData(OfficeHour officeHour) {
        return new OfficeHourData(officeHour.dayOfWeek().name(), officeHour.startTime(), officeHour.endTime());
    }
}
