package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IProductDataSource;
import com.postech.restaurantes.adapter.datasource.data.ProductData;
import com.postech.restaurantes.adapter.datasource.data.ProductOptionGroupData;
import com.postech.restaurantes.adapter.datasource.data.ProductOptionValueData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.entity.product.ProductOptionGroup;
import com.postech.restaurantes.domain.entity.product.ProductOptionValue;
import java.util.Optional;
import java.util.UUID;

/** Traduz entre o agregado de produto e os registros da origem de dados. */
public final class ProductGateway implements IProductGateway {

    private final IProductDataSource dataSource;

    private ProductGateway(IProductDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de produto inválida");
    }

    public static ProductGateway create(IProductDataSource dataSource) {
        return new ProductGateway(dataSource);
    }

    @Override
    public Optional<Product> findById(UUID restaurantId, UUID productId) {
        return dataSource.findById(restaurantId, productId).map(ProductGateway::toEntity);
    }

    @Override
    public PageResult<Product> search(UUID restaurantId, PageRequest request) {
        return dataSource.search(restaurantId, request).map(ProductGateway::toEntity);
    }

    @Override
    public Product insert(Product product) {
        return toEntity(dataSource.insert(toData(product)));
    }

    @Override
    public Product update(Product product) {
        return toEntity(dataSource.update(toData(product)));
    }

    @Override
    public void delete(UUID restaurantId, UUID productId) {
        dataSource.delete(restaurantId, productId);
    }

    static Product toEntity(ProductData data) {
        return Product.restore(data.id(), data.restaurantId(), data.name(), data.description(), data.price(),
                data.dineInOnly(), data.imagePaths(), data.optionGroups().stream()
                        .map(group -> new ProductOptionGroup(group.name(), group.required(),
                                group.minSelections(), group.maxSelections(), group.values().stream()
                                .map(value -> new ProductOptionValue(value.name(), value.additionalPrice()))
                                .toList()))
                        .toList());
    }

    static ProductData toData(Product product) {
        return new ProductData(product.getId(), product.getRestaurantId(), product.getName(),
                product.getDescription(), product.getPrice(), product.isDineInOnly(), product.getImagePaths(),
                product.getOptionGroups().stream().map(group -> new ProductOptionGroupData(
                        group.name(), group.required(), group.minSelections(), group.maxSelections(),
                        group.values().stream().map(value ->
                                new ProductOptionValueData(value.name(), value.additionalPrice())).toList()))
                        .toList());
    }
}
