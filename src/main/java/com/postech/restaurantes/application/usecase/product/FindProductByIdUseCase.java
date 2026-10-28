package com.postech.restaurantes.application.usecase.product;

import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Consulta um produto dentro do restaurante informado. */
public final class FindProductByIdUseCase {

    private final IProductGateway products;

    private FindProductByIdUseCase(IProductGateway products) {
        this.products = products;
    }

    public static FindProductByIdUseCase create(IProductGateway products) {
        return new FindProductByIdUseCase(products);
    }

    public Product run(UUID restaurantId, UUID productId) {
        return products.findById(restaurantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado"));
    }
}
