package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.ProductView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.product.Product;

/** Prepara a saída do agregado de produto em views. */
public final class ProductPresenter {

    private ProductPresenter() {
    }

    public static ProductView toView(Product product) {
        Guard.requireNonNull(product, "Produto inválido");
        return new ProductView(product.getId(), product.getRestaurantId(), product.getName(),
                product.getDescription(), product.getPrice(), product.isDineInOnly(), product.getImagePaths(),
                product.getOptionGroups().stream().map(group -> new ProductView.ProductOptionGroupView(
                        group.name(), group.required(), group.minSelections(), group.maxSelections(),
                        group.values().stream().map(value -> new ProductView.ProductOptionValueView(
                                value.name(), value.additionalPrice())).toList())).toList());
    }

    public static PageResult<ProductView> toView(PageResult<Product> page) {
        Guard.requireNonNull(page, "Página inválida");
        return page.map(ProductPresenter::toView);
    }
}
