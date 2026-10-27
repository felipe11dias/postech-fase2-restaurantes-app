package com.postech.restaurantes.adapter.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.IProductDataSource;
import com.postech.restaurantes.adapter.datasource.data.ProductData;
import com.postech.restaurantes.adapter.datasource.data.ProductOptionGroupData;
import com.postech.restaurantes.adapter.datasource.data.ProductOptionValueData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.product.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProductGatewayTest {

    private IProductDataSource dataSource;
    private ProductGateway gateway;
    private UUID restaurantId;
    private UUID productId;
    private ProductData data;

    @BeforeEach
    void setUp() {
        dataSource = mock(IProductDataSource.class);
        gateway = ProductGateway.create(dataSource);
        restaurantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        data = new ProductData(productId, restaurantId, "Pizza", "Descrição", new BigDecimal("20.00"),
                false, List.of("/images/pizza.jpg"),
                List.of(new ProductOptionGroupData("Tamanho", true, 1, 1,
                        List.of(new ProductOptionValueData("Grande", new BigDecimal("2.50"))))));
    }

    @Test
    @DisplayName("Busca produto e reconstrói as opções pelo domínio")
    void deveBuscarProduto() {
        when(dataSource.findById(restaurantId, productId)).thenReturn(Optional.of(data));

        Product product = gateway.findById(restaurantId, productId).orElseThrow();

        assertEquals("Grande", product.getOptionGroups().getFirst().values().getFirst().name());
        assertEquals(new BigDecimal("2.50"),
                product.getOptionGroups().getFirst().values().getFirst().additionalPrice());
    }

    @Test
    @DisplayName("Busca produto ausente e traduz página")
    void deveBuscarAusenteEPagina() {
        PageRequest request = PageRequest.of(0, 10);
        when(dataSource.findById(restaurantId, productId)).thenReturn(Optional.empty());
        when(dataSource.search(restaurantId, request)).thenReturn(new PageResult<>(List.of(data), 0, 10, 1));

        assertTrue(gateway.findById(restaurantId, productId).isEmpty());
        assertEquals("Pizza", gateway.search(restaurantId, request).content().getFirst().getName());
    }

    @Test
    @DisplayName("Insere e atualiza traduzindo o produto para o registro")
    void devePersistirProduto() {
        when(dataSource.insert(any())).thenReturn(data);
        when(dataSource.update(any())).thenReturn(data);
        Product product = Product.restore(productId, restaurantId, "Pizza", "Descrição",
                new BigDecimal("20.00"), false, List.of("/images/pizza.jpg"),
                List.of(new com.postech.restaurantes.domain.entity.product.ProductOptionGroup(
                        "Tamanho", true, 1, 1, List.of(new com.postech.restaurantes.domain.entity.product
                                .ProductOptionValue("Grande", new BigDecimal("2.50"))))));

        gateway.insert(product);
        gateway.update(product);

        ArgumentCaptor<ProductData> captor = ArgumentCaptor.forClass(ProductData.class);
        verify(dataSource).insert(captor.capture());
        assertEquals("Tamanho", captor.getValue().optionGroups().getFirst().name());
        assertEquals("/images/pizza.jpg", captor.getValue().imagePaths().getFirst());
    }

    @Test
    @DisplayName("Exclui produto pela origem de dados")
    void deveExcluirProduto() {
        gateway.delete(restaurantId, productId);
        verify(dataSource).delete(restaurantId, productId);
    }

    @Test
    @DisplayName("Recusa origem de dados nula")
    void deveRecusarOrigemNula() {
        assertThrows(IllegalArgumentException.class, () -> ProductGateway.create(null));
    }
}
