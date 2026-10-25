package com.postech.restaurantes.adapter.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.postech.restaurantes.adapter.AdapterFixtures.ADDRESS_DATA;
import static com.postech.restaurantes.adapter.AdapterFixtures.NOW;
import static com.postech.restaurantes.adapter.AdapterFixtures.OFFICE_HOURS_DATA;

import com.postech.restaurantes.adapter.datasource.IProductDataSource;
import com.postech.restaurantes.adapter.datasource.IRestaurantDataSource;
import com.postech.restaurantes.adapter.datasource.data.ProductData;
import com.postech.restaurantes.adapter.datasource.data.RestaurantData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.product.ProductDTO;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductControllerTest {

    private IProductDataSource products;
    private IRestaurantDataSource restaurants;
    private IUnitOfWork unitOfWork;
    private ProductController controller;
    private UUID restaurantId;
    private UUID productId;
    private ProductData data;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        products = mock(IProductDataSource.class);
        restaurants = mock(IRestaurantDataSource.class);
        unitOfWork = mock(IUnitOfWork.class);
        when(unitOfWork.execute(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(unitOfWork).execute(any(Runnable.class));
        controller = ProductController.create(products, restaurants, unitOfWork);
        restaurantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        data = new ProductData(productId, restaurantId, "Pizza", null, 10.0,
                false, List.of(), List.of());
    }

    @Test
    @DisplayName("Cria produto por meio da unidade de trabalho")
    void deveCriarProduto() {
        when(restaurants.findById(restaurantId)).thenReturn(Optional.of(
                restaurantData()));
        when(products.insert(any())).thenReturn(data);
        assertNotNull(controller.create(restaurantId,
                new ProductDTO("Pizza", null, 10.0, false, List.of(), List.of())));
    }

    @Test
    @DisplayName("Consulta produto e página")
    void deveConsultarProdutoEPagina() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.of(data));
        when(restaurants.findById(restaurantId)).thenReturn(Optional.of(
                restaurantData()));
        when(products.search(restaurantId, PageRequest.of(0, 10)))
                .thenReturn(new PageResult<>(List.of(data), 0, 10, 1));
        assertNotNull(controller.findById(restaurantId, productId));
        assertNotNull(controller.search(restaurantId, PageRequest.of(0, 10)));
    }

    @Test
    @DisplayName("Atualiza e exclui produto")
    void deveAtualizarEExcluirProduto() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.of(data));
        when(products.update(any())).thenReturn(data);
        ProductDTO dto = new ProductDTO("Pizza nova", null, 10.0, false, List.of(), List.of());
        assertNotNull(controller.update(restaurantId, productId, dto));
        controller.delete(restaurantId, productId);
    }

    @Test
    @DisplayName("Recusa dependências nulas")
    void deveRecusarDependenciasNulas() {
        assertThrows(IllegalArgumentException.class, () -> ProductController.create(null, restaurants, unitOfWork));
        assertThrows(IllegalArgumentException.class, () -> ProductController.create(products, null, unitOfWork));
        assertThrows(IllegalArgumentException.class, () -> ProductController.create(products, restaurants, null));
    }

    private RestaurantData restaurantData() {
        return new RestaurantData(UUID.randomUUID(), UUID.randomUUID(), ADDRESS_DATA, "Restaurante",
                OFFICE_HOURS_DATA, NOW, NOW);
    }
}
