package com.postech.restaurantes.adapter.datasource.data;

import java.util.List;
import java.util.UUID;

/** Registro do produto e das partes que a origem de dados devolve. */
public record ProductData(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        double price,
        boolean dineInOnly,
        List<String> imagePaths,
        List<ProductOptionGroupData> optionGroups
) {
}
