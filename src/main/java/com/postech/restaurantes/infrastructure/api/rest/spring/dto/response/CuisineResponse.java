package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import java.time.LocalDateTime;
import java.util.UUID;

public record CuisineResponse(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {

    public static CuisineResponse from(CuisineView view) {
        return new CuisineResponse(view.id(), view.name(), view.createdAt(), view.lastUpdatedAt());
    }
}