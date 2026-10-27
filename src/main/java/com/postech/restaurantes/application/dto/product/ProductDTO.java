package com.postech.restaurantes.application.dto.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.postech.restaurantes.domain.entity.product.Product;

/** Dados completos para criação ou substituição de um produto e de suas opções. */
public record ProductDTO(
        String name,
        String description,
        BigDecimal price,
        boolean dineInOnly,
        List<String> imagePaths,
        List<ProductOptionGroupDTO> optionGroups
) {

    public Product toProduct(UUID restaurantId) {
        return Product.create(restaurantId, name, description,
                price, dineInOnly, imagePaths, ProductMapping.toGroups(optionGroups));
    }
}
