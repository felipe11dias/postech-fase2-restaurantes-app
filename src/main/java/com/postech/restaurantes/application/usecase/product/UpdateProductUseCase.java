package com.postech.restaurantes.application.usecase.product;

import com.postech.restaurantes.application.dto.product.ProductDTO;
import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Substitui os dados do produto e toda a hierarquia de opções enviada. */
public final class UpdateProductUseCase {

    private final IProductGateway products;

    private UpdateProductUseCase(IProductGateway products) {
        this.products = products;
    }

    public static UpdateProductUseCase create(IProductGateway products) {
        return new UpdateProductUseCase(products);
    }

    public Product run(UUID restaurantId, UUID productId, ProductDTO dto) {
        Guard.requireNonNull(dto, "Dados do produto inválidos");
        Product product = products.findById(restaurantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
        Product replacement = dto.toProduct(product.getRestaurantId());
        product.setName(replacement.getName());
        product.setDescription(replacement.getDescription());
        product.setPrice(replacement.getPrice());
        product.setDineInOnly(replacement.isDineInOnly());
        product.replaceImagePaths(replacement.getImagePaths());
        product.replaceOptionGroups(replacement.getOptionGroups());
        return products.update(product);
    }
}
