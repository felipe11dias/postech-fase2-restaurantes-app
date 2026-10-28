package com.postech.restaurantes.application.usecase.product;

import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Exclui um produto do restaurante informado. */
public final class DeleteProductUseCase {

    private final IProductGateway products;

    private DeleteProductUseCase(IProductGateway products) {
        this.products = products;
    }

    public static DeleteProductUseCase create(IProductGateway products) {
        return new DeleteProductUseCase(products);
    }

    public void run(UUID restaurantId, UUID productId) {
        if (products.findById(restaurantId, productId).isEmpty()) {
            throw new ResourceNotFoundException("Produto não encontrado");
        }
        products.delete(restaurantId, productId);
    }
}
