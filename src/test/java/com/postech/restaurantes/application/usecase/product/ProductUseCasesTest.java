package com.postech.restaurantes.application.usecase.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.dto.product.ProductDTO;
import com.postech.restaurantes.application.dto.product.ProductOptionGroupDTO;
import com.postech.restaurantes.application.dto.product.ProductOptionValueDTO;
import com.postech.restaurantes.application.gateway.IProductGateway;
import com.postech.restaurantes.application.gateway.IRestaurantGateway;
import com.postech.restaurantes.domain.entity.product.Product;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProductUseCasesTest {

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();
    private IProductGateway products;
    private IRestaurantGateway restaurants;
    private ProductDTO dto;
    private Product product;

    @BeforeEach
    void setUp() {
        products = mock(IProductGateway.class);
        restaurants = mock(IRestaurantGateway.class);
        dto = new ProductDTO("Pizza", null, new BigDecimal("20.00"), false, List.of("/images/pizza.jpg"),
                List.of(new ProductOptionGroupDTO("Tamanho", false, 0, 1,
                        List.of(new ProductOptionValueDTO("Grande", BigDecimal.ZERO)))));
        product = Product.restore(productId, restaurantId, "Pizza", null, new BigDecimal("20.00"),
                false, List.of("/images/pizza.jpg"), List.of());
        when(restaurants.findById(restaurantId)).thenReturn(Optional.of(
                Restaurant.restore(UUID.randomUUID(), UUID.randomUUID(),
                        com.postech.restaurantes.application.usecase.UseCaseFixtures.address(),
                        "Restaurante", com.postech.restaurantes.application.usecase.UseCaseFixtures.OFFICE_HOURS,
                        null, null)));
    }

    @Test
    @DisplayName("Cria produto para restaurante existente")
    void deveCriarProduto() {
        when(products.insert(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Product created = CreateProductUseCase.create(products, restaurants).run(restaurantId, dto);
        assertEquals("Pizza", created.getName());
        assertEquals(restaurantId, created.getRestaurantId());
        assertEquals("Grande", created.getOptionGroups().getFirst().values().getFirst().name());
    }

    @Test
    @DisplayName("Recusa criação com dados nulos ou restaurante inexistente")
    void deveRecusarCriacaoInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> CreateProductUseCase.create(products, restaurants).run(restaurantId, null));
        when(restaurants.findById(restaurantId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> CreateProductUseCase.create(products, restaurants).run(restaurantId, dto));
    }

    @Test
    @DisplayName("Consulta produto dentro do restaurante")
    void deveBuscarProduto() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.of(product));
        assertEquals(product, FindProductByIdUseCase.create(products).run(restaurantId, productId));
    }

    @Test
    @DisplayName("Consulta produto inexistente")
    void deveRecusarProdutoInexistente() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> FindProductByIdUseCase.create(products).run(restaurantId, productId));
    }

    @Test
    @DisplayName("Lista produtos paginados após validar restaurante")
    void deveListarProdutos() {
        PageRequest request = PageRequest.of(0, 10);
        PageResult<Product> page = new PageResult<>(List.of(product), 0, 10, 1);
        when(products.search(restaurantId, request)).thenReturn(page);
        assertEquals(page, SearchProductsUseCase.create(products, restaurants).run(restaurantId, request));
    }

    @Test
    @DisplayName("Recusa busca sem página ou restaurante existente")
    void deveRecusarBuscaInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> SearchProductsUseCase.create(products, restaurants).run(restaurantId, null));
        when(restaurants.findById(restaurantId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> SearchProductsUseCase.create(products, restaurants).run(restaurantId, PageRequest.first()));
    }

    @Test
    @DisplayName("Atualiza dados e substitui opções completas")
    void deveAtualizarProduto() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.of(product));
        when(products.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Product updated = UpdateProductUseCase.create(products).run(restaurantId, productId, dto);
        assertEquals("Pizza", updated.getName());
        assertEquals(1, updated.getImagePaths().size());
        assertEquals("/images/pizza.jpg", updated.getImagePaths().getFirst());
        assertEquals(1, updated.getOptionGroups().size());
    }

    @Test
    @DisplayName("Recusa atualização sem dados ou produto")
    void deveRecusarAtualizacaoInvalida() {
        assertThrows(IllegalArgumentException.class,
                () -> UpdateProductUseCase.create(products).run(restaurantId, productId, null));
        when(products.findById(restaurantId, productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> UpdateProductUseCase.create(products).run(restaurantId, productId, dto));
    }

    @Test
    @DisplayName("Exclui produto existente")
    void deveExcluirProduto() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.of(product));
        DeleteProductUseCase.create(products).run(restaurantId, productId);
        verify(products).delete(restaurantId, productId);
    }

    @Test
    @DisplayName("Recusa exclusão de produto inexistente")
    void deveRecusarExclusaoInvalida() {
        when(products.findById(restaurantId, productId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> DeleteProductUseCase.create(products).run(restaurantId, productId));
    }
}
