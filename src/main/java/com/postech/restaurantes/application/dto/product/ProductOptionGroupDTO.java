package com.postech.restaurantes.application.dto.product;

import java.util.List;

/** Dados de um grupo de opções recebido pelo caso de uso. */
public record ProductOptionGroupDTO(
        String name,
        boolean required,
        int minSelections,
        int maxSelections,
        List<ProductOptionValueDTO> values
) {
}
