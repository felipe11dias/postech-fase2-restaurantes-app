package com.postech.restaurantes.adapter.controller;

import com.postech.restaurantes.adapter.datasource.IProductDataSource;
import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.gateway.ProductGateway;
import com.postech.restaurantes.adapter.gateway.RestaurantGateway;
import com.postech.restaurantes.adapter.presenter.ProductPresenter;
import com.postech.restaurantes.adapter.presenter.view.ProductView;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.product.ProductDTO;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.application.usecase.product.CreateProductUseCase;
import com.postech.restaurantes.application.usecase.product.DeleteProductUseCase;
import com.postech.restaurantes.application.usecase.product.FindProductByIdUseCase;
import com.postech.restaurantes.application.usecase.product.SearchProductsUseCase;
import com.postech.restaurantes.application.usecase.product.UpdateProductUseCase;
import com.postech.restaurantes.domain.Guard;
import java.util.UUID;

/** Controller de adaptação do agregado de produto. */
public final class ProductController {

    private final IProductDataSource productDataSource;
    private final IRestaurantDataSource restaurantDataSource;
    private final IUnitOfWork unitOfWork;

    private ProductController(IProductDataSource productDataSource, IRestaurantDataSource restaurantDataSource,
                              IUnitOfWork unitOfWork) {
        this.productDataSource = Guard.requireNonNull(productDataSource, "Origem de dados de produto inválida");
        this.restaurantDataSource = Guard.requireNonNull(restaurantDataSource, "Origem de dados de restaurante inválida");
        this.unitOfWork = Guard.requireNonNull(unitOfWork, "Unidade de trabalho inválida");
    }

    public static ProductController create(IProductDataSource productDataSource,
                                           IRestaurantDataSource restaurantDataSource, IUnitOfWork unitOfWork) {
        return new ProductController(productDataSource, restaurantDataSource, unitOfWork);
    }

    public ProductView create(UUID restaurantId, ProductDTO dto) {
        var useCase = CreateProductUseCase.create(productGateway(), restaurantGateway());
        return ProductPresenter.toView(unitOfWork.execute(() -> useCase.run(restaurantId, dto)));
    }

    public ProductView findById(UUID restaurantId, UUID productId) {
        var useCase = FindProductByIdUseCase.create(productGateway());
        return ProductPresenter.toView(unitOfWork.execute(() -> useCase.run(restaurantId, productId)));
    }

    public PageResult<ProductView> search(UUID restaurantId, PageRequest request) {
        var useCase = SearchProductsUseCase.create(productGateway(), restaurantGateway());
        return ProductPresenter.toView(unitOfWork.execute(() -> useCase.run(restaurantId, request)));
    }

    public ProductView update(UUID restaurantId, UUID productId, ProductDTO dto) {
        var useCase = UpdateProductUseCase.create(productGateway());
        return ProductPresenter.toView(unitOfWork.execute(() -> useCase.run(restaurantId, productId, dto)));
    }

    public void delete(UUID restaurantId, UUID productId) {
        var useCase = DeleteProductUseCase.create(productGateway());
        unitOfWork.execute(() -> useCase.run(restaurantId, productId));
    }

    private ProductGateway productGateway() {
        return ProductGateway.create(productDataSource);
    }

    private RestaurantGateway restaurantGateway() {
        return RestaurantGateway.create(restaurantDataSource);
    }
}
