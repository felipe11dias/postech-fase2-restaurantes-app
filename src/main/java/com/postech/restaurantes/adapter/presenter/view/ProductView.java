package com.postech.restaurantes.adapter.presenter.view;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Dados de produto que o cliente pode consultar. */
public record ProductView(
        UUID id,
        UUID restaurantId,
        String name,
        String description,
        double price,
        boolean dineInOnly,
        List<String> imagePaths,
        List<ProductOptionGroupView> optionGroups
) {

    public record ProductOptionGroupView(
            String name,
            boolean required,
            int minSelections,
            int maxSelections,
            List<ProductOptionValueView> values
    ) {
    }

    public record ProductOptionValueView(String name, BigDecimal additionalPrice) {
    }
}
