package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.ProductView;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Produto com suas imagens e opções no corpo da resposta HTTP. */
public record ProductResponse(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        BigDecimal price,
        boolean dineInOnly,
        List<String> imagePaths,
        List<ProductOptionGroupResponse> optionGroups
) {

    public static ProductResponse from(ProductView view) {
        return new ProductResponse(view.id(), view.restaurantId(), view.name(), view.description(), view.price(),
                view.dineInOnly(), view.imagePaths(), view.optionGroups().stream()
                        .map(group -> new ProductOptionGroupResponse(group.name(), group.required(),
                                group.minSelections(), group.maxSelections(),
                                group.values().stream().map(value ->
                                        new ProductOptionValueResponse(value.name(), value.additionalPrice()))
                                        .toList()))
                        .toList());
    }

    public record ProductOptionGroupResponse(
            String name,
            boolean required,
            int minSelections,
            int maxSelections,
            List<ProductOptionValueResponse> values
    ) {
    }

    public record ProductOptionValueResponse(String name, BigDecimal additionalPrice) {
    }
}
