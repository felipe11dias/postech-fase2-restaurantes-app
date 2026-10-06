package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant;

import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.common.SortDirection;
import com.postech.restaurantes.infrastructure.persistence.jpa.address.AddressJpaMapping;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Implementação JPA da origem de dados de restaurantes. */
@Repository
public class RestaurantDataSourceJpa implements IRestaurantDataSource {

    private static final Map<String, String> SORT_PROPERTIES = Map.of(
            "id", "id",
            "name", "name",
            "createdAt", "createdAt",
            "lastUpdatedAt", "lastUpdatedAt");

    private static final String DEFAULT_SORT_PROPERTY = "name";

    private final SpringDataRestaurantRepository restaurants;

    public RestaurantDataSourceJpa(SpringDataRestaurantRepository restaurants) {
        this.restaurants = restaurants;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RestaurantData> findById(UUID id) {
        return restaurants.findById(id).map(RestaurantDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RestaurantData> search(String name, PageRequest request) {
        Sort sort = toSort(request);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.page(), request.size(), sort);
        Page<UUID> ids = restaurants.findIdsByName(name == null ? "" : name, pageable);
        List<RestaurantData> content = ids.isEmpty()
                ? List.of()
                : restaurants.findByIdIn(ids.getContent(), sort).stream().map(RestaurantDataSourceJpa::toData).toList();
        return new PageResult<>(content, request.page(), request.size(), ids.getTotalElements());
    }

    @Override
    @Transactional
    public RestaurantData insert(RestaurantData restaurant) {
        RestaurantJpaEntity entity = new RestaurantJpaEntity();
        apply(entity, restaurant);
        return toData(restaurants.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public RestaurantData update(RestaurantData restaurant) {
        RestaurantJpaEntity entity = restaurants.findById(restaurant.id())
                .orElseThrow(() -> new IllegalStateException("Restaurante inexistente para atualização: " + restaurant.id()));
        apply(entity, restaurant);
        return toData(restaurants.saveAndFlush(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(UUID userId) {
        return restaurants.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        restaurants.deleteById(id);
    }

    /**
     * Copia o registro para a entidade gerenciada. O endereço do restaurante existente é
     * atualizado na mesma linha (o id não muda); só o restaurante novo ganha uma linha nova.
     */
    private void apply(RestaurantJpaEntity entity, RestaurantData data) {
        entity.setUserId(data.userId());
        if (entity.getAddress() == null) {
            entity.setAddress(AddressJpaMapping.toEntity(data.address()));
        } else {
            AddressJpaMapping.copy(data.address(), entity.getAddress());
        }
        entity.setName(data.name());
        entity.setOfficeHourStart(data.officeHourStart());
        entity.setOfficeHourEnd(data.officeHourEnd());
    }

    static RestaurantData toData(RestaurantJpaEntity entity) {
        return new RestaurantData(
                entity.getId(),
                entity.getUserId(),
                AddressJpaMapping.toData(entity.getAddress()),
                entity.getName(),
                entity.getOfficeHourStart(),
                entity.getOfficeHourEnd(),
                entity.getCreatedAt(),
                entity.getLastUpdatedAt()
        );
    }

    private static Sort toSort(PageRequest request) {
        String property = request.hasSort()
                ? SORT_PROPERTIES.getOrDefault(request.sortBy(), DEFAULT_SORT_PROPERTY)
                : DEFAULT_SORT_PROPERTY;
        Sort.Direction direction = request.direction() == SortDirection.DESC
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        return Sort.by(direction, property);
    }
}
