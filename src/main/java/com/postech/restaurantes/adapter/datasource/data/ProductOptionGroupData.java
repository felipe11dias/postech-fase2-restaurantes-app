package com.postech.restaurantes.adapter.datasource.data;

import java.util.List;

/** Registro de grupo de opções. */
public record ProductOptionGroupData(
        String name,
        boolean required,
        int minSelections,
        int maxSelections,
        List<ProductOptionValueData> values
) {
}
