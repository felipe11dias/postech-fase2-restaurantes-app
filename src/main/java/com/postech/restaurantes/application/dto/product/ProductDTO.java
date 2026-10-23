package com.postech.restaurantes.application.dto.product;

import java.util.List;
import java.util.UUID;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.product.Product;

/** Dados completos para criação ou substituição de um produto e de suas opções. */
public record ProductDTO(
        String name,
        String description,
        Double price,
        boolean dineInOnly,
        List<String> imagePaths,
        List<ProductOptionGroupDTO> optionGroups
) {

    public Product toProduct(UUID restaurantId) {
        return Product.create(restaurantId, name, description,
                Guard.requireNonNull(price, "Preço do produto inválido"), dineInOnly, imagePaths,
                ProductMapping.toGroups(optionGroups));
    }
}
