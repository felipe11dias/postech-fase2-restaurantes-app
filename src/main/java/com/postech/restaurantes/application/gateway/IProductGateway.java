package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.product.Product;
import java.util.Optional;
import java.util.UUID;

/** Porta de acesso aos produtos e às partes do agregado. */
public interface IProductGateway {

    Optional<Product> findById(UUID restaurantId, UUID productId);

    PageResult<Product> search(UUID restaurantId, PageRequest request);

    Product insert(Product product);

    Product update(Product product);

    void delete(UUID restaurantId, UUID productId);
}
