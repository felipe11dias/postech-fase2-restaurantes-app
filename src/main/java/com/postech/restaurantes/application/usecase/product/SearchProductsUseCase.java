package com.postech.restaurantes.application.usecase.product;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.UUID;

/** Lista os produtos de um restaurante de forma paginada. */
public final class SearchProductsUseCase {

    private final IProductGateway products;
    private final IRestaurantGateway restaurants;

    private SearchProductsUseCase(IProductGateway products, IRestaurantGateway restaurants) {
        this.products = products;
        this.restaurants = restaurants;
    }

    public static SearchProductsUseCase create(IProductGateway products, IRestaurantGateway restaurants) {
        return new SearchProductsUseCase(products, restaurants);
    }

    public PageResult<Product> run(UUID restaurantId, PageRequest request) {
        Guard.requireNonNull(request, "Parâmetros de paginação inválidos");
        restaurants.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurante não encontrado"));
        return products.search(restaurantId, request);
    }
}
