package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.CuisineData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICuisineDataSource {

    Optional<CuisineData> findById(UUID id);

    Optional<CuisineData> findByName(String name);

    List<CuisineData> findAllById(Collection<UUID> ids);

    PageResult<CuisineData> search(String name, PageRequest request);

    CuisineData insert(CuisineData data);

    CuisineData update(CuisineData data);

    boolean existsInRestaurants(UUID id);

    void delete(UUID id);
}