package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDateTime;
import java.util.UUID;

public record CuisineView(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}