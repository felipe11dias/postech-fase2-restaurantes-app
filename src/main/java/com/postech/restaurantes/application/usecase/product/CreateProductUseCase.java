package com.postech.restaurantes.application.usecase.product;

import com.postech.restaurantes.application.dto.product.ProductDTO;
import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Cadastra produto para um restaurante existente. */
public final class CreateProductUseCase {

    private final IProductGateway products;
    private final IRestaurantGateway restaurants;

    private CreateProductUseCase(IProductGateway products, IRestaurantGateway restaurants) {
        this.products = products;
        this.restaurants = restaurants;
    }

    public static CreateProductUseCase create(IProductGateway products, IRestaurantGateway restaurants) {
        return new CreateProductUseCase(products, restaurants);
    }

    public Product run(UUID restaurantId, ProductDTO dto) {
        Guard.requireNonNull(dto, "Dados do produto inválidos");
        restaurants.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        return products.insert(dto.toProduct(restaurantId));
    }
}
