package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalDateTime;
import java.util.UUID;

public record CuisineData(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime lastUpdatedAt
) {
}