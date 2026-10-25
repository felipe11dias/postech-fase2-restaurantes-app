package com.postech.restaurantes.adapter.presenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.entity.product.ProductOptionGroup;
import com.postech.restaurantes.domain.entity.product.ProductOptionValue;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductPresenterTest {

    @Test
    @DisplayName("Apresenta produto sem expor tipos do domínio")
    void deveApresentarProduto() {
        Product product = Product.restore(UUID.randomUUID(), UUID.randomUUID(), "Pizza", null, 10.0,
                false, List.of("/images/pizza.jpg"),
                List.of(new ProductOptionGroup("Tamanho", false, 0, 1,
                        List.of(new ProductOptionValue("Grande", BigDecimal.ZERO)))));

        var view = ProductPresenter.toView(product);

        assertEquals("Pizza", view.name());
        assertEquals("Tamanho", view.optionGroups().getFirst().name());
        assertEquals("Grande", view.optionGroups().getFirst().values().getFirst().name());
    }

    @Test
    @DisplayName("Apresenta página de produtos")
    void deveApresentarPagina() {
        Product product = Product.create(UUID.randomUUID(), "Pizza", null, 10.0,
                false, List.of(), List.of());
        PageResult<Product> page = new PageResult<>(List.of(product), 0, 10, 1);
        assertEquals("Pizza", ProductPresenter.toView(page).content().getFirst().name());
    }

    @Test
    @DisplayName("Recusa produto ou página nulos")
    void deveRecusarNulos() {
        assertThrows(IllegalArgumentException.class, () -> ProductPresenter.toView((Product) null));
        assertThrows(IllegalArgumentException.class, () -> ProductPresenter.toView((PageResult<Product>) null));
    }
}
