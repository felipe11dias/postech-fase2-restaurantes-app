package com.postech.restaurantes.adapter.datasource;

import com.postech.restaurantes.adapter.datasource.data.ProductData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.Optional;
import java.util.UUID;

/** Origem de dados do catálogo de produtos. */
public interface IProductDataSource {

    Optional<ProductData> findById(UUID restaurantId, UUID productId);

    PageResult<ProductData> search(UUID restaurantId, PageRequest request);

    ProductData insert(ProductData data);

    ProductData update(ProductData data);

    void delete(UUID restaurantId, UUID productId);
}
