package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.Optional;
import java.util.UUID;

/** Origem de dados de restaurante. */
public interface IRestaurantDataSource {

    Optional<RestaurantData> findById(UUID id);

    PageResult<RestaurantData> search(String name, PageRequest request);

    RestaurantData insert(RestaurantData data);

    RestaurantData update(RestaurantData data);

    boolean existsByUserId(UUID userId);

    void delete(UUID id);
}
