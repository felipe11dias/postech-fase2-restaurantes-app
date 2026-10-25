package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICuisineGateway {

    Optional<Cuisine> findById(UUID id);

    Optional<Cuisine> findByName(String name);

    List<Cuisine> findAllById(Collection<UUID> ids);

    PageResult<Cuisine> search(String name, PageRequest request);

    Cuisine insert(Cuisine cuisine);

    Cuisine update(Cuisine cuisine);

    boolean isUsedByRestaurants(UUID id);

    void delete(UUID id);
}
