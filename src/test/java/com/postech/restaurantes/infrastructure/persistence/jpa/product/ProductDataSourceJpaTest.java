package com.postech.restaurantes.infrastructure.persistence.jpa.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.postech.restaurantes.adapter.datasource.data.ProductData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.image.ProductImageJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.optiongroup.ProductOptionGroupJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.optionvalue.ProductOptionValueJpaEntity;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

class ProductDataSourceJpaTest {

    private SpringDataProductRepository repository;
    private ProductDataSourceJpa dataSource;
    private UUID restaurantId;
    private UUID productId;
    private ProductJpaEntity entity;
    private ProductData data;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataProductRepository.class);
        dataSource = new ProductDataSourceJpa(repository);
        restaurantId = UUID.randomUUID();
        productId = UUID.randomUUID();
        entity = entity();
        data = new ProductData(productId, restaurantId, "Pizza", "Descrição", 20.0,
                false, List.of("/images/pizza.jpg"), List.of());
    }

    @Test
    @DisplayName("Busca produto existente e inexistente pelo restaurante")
    void deveBuscarPorIds() {
        when(repository.findByIdAndRestaurantId(productId, restaurantId)).thenReturn(Optional.of(entity));
        UUID missingProductId = UUID.randomUUID();
        when(repository.findByIdAndRestaurantId(missingProductId, restaurantId)).thenReturn(Optional.empty());

        assertEquals("Pizza", dataSource.findById(restaurantId, productId).orElseThrow().name());
        assertEquals(Optional.empty(), dataSource.findById(restaurantId, missingProductId));
    }

    @Test
    @DisplayName("Busca página vazia e página com produtos")
    void deveBuscarPaginas() {
        PageRequest request = PageRequest.of(0, 10);
        when(repository.findIdsByRestaurantId(restaurantId, org.springframework.data.domain.PageRequest.of(0, 10,
                Sort.by(Sort.Direction.ASC, "name"))))
                .thenReturn(new PageImpl<>(List.<UUID>of(), org.springframework.data.domain.PageRequest.of(0, 10), 0))
                .thenReturn(new PageImpl<>(List.of(productId), org.springframework.data.domain.PageRequest.of(0, 10), 1));
        when(repository.findByIdIn(List.of(productId), Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(entity));

        assertEquals(0, dataSource.search(restaurantId, request).totalElements());
        assertEquals("Pizza", dataSource.search(restaurantId, request).content().getFirst().name());
    }

    @Test
    @DisplayName("Insere produto com seus caminhos e opções")
    void deveInserirProduto() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ProductData withGroups = dataWithGroups();

        ProductData saved = dataSource.insert(withGroups);

        assertEquals("Pizza", saved.name());
        assertEquals("Grande", saved.optionGroups().getFirst().values().getFirst().name());
        ArgumentCaptor<ProductJpaEntity> captor = ArgumentCaptor.forClass(ProductJpaEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("/images/pizza.jpg", captor.getValue().getImages().iterator().next().getContent());
        assertEquals("Tamanho", captor.getValue().getOptionGroups().iterator().next().getName());
    }

    @Test
    @DisplayName("Atualiza produto gerenciado e recusa produto ausente")
    void deveAtualizarProduto() {
        when(repository.findByIdAndRestaurantId(productId, restaurantId)).thenReturn(Optional.of(entity));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("Pizza", dataSource.update(data).name());
        when(repository.findByIdAndRestaurantId(productId, restaurantId)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> dataSource.update(data));
    }

    @Test
    @DisplayName("Exclui quando produto existe e ignora produto ausente")
    void deveExcluirProduto() {
        when(repository.findByIdAndRestaurantId(productId, restaurantId)).thenReturn(Optional.of(entity));
        dataSource.delete(restaurantId, productId);
        verify(repository).delete(entity);
        when(repository.findByIdAndRestaurantId(productId, restaurantId)).thenReturn(Optional.empty());
        dataSource.delete(restaurantId, productId);
    }

    @Test
    @DisplayName("Traduz entidades JPA e todas as partes para registros")
    void deveMapearEntidadesJpa() {
        ProductData mapped = ProductDataSourceJpa.toData(entity);
        assertEquals(1, mapped.imagePaths().size());
        assertEquals("/images/pizza.jpg", mapped.imagePaths().getFirst());
        assertEquals("Tamanho", mapped.optionGroups().getFirst().name());
        assertEquals("Grande", mapped.optionGroups().getFirst().values().getFirst().name());
        ProductJpaEntity root = entity();
        ProductImageJpaEntity image = root.getImages().iterator().next();
        ProductOptionGroupJpaEntity group = root.getOptionGroups().iterator().next();
        ProductOptionValueJpaEntity value = group.getValues().iterator().next();
        assertEquals(null, root.getId());
        assertEquals(null, image.getId());
        assertEquals(null, group.getId());
        assertEquals(null, value.getId());
    }

    private ProductData dataWithGroups() {
        return new ProductData(productId, restaurantId, "Pizza", "Descrição", 20.0,
                false, List.of("/images/pizza.jpg"),
                List.of(new com.postech.restaurantes.adapter.datasource.data.ProductOptionGroupData(
                        "Tamanho", true, 1, 1,
                        List.of(new com.postech.restaurantes.adapter.datasource.data.ProductOptionValueData(
                                "Grande", new BigDecimal("2.50"))))));
    }

    private ProductJpaEntity entity() {
        ProductJpaEntity product = new ProductJpaEntity();
        product.setRestaurantId(restaurantId);
        product.setName("Pizza");
        product.setDescription("Descrição");
        product.setPrice(20.0);
        product.setDineInOnly(false);
        ProductImageJpaEntity image = new ProductImageJpaEntity();
        image.setContent("/images/pizza.jpg");
        product.replaceImages(Set.of(image));
        ProductOptionGroupJpaEntity group = new ProductOptionGroupJpaEntity();
        group.setName("Tamanho");
        group.setRequired(true);
        group.setMinSelections(1);
        group.setMaxSelections(1);
        ProductOptionValueJpaEntity value = new ProductOptionValueJpaEntity();
        value.setName("Grande");
        value.setAdditionalPrice(new BigDecimal("2.50"));
        group.replaceValues(Set.of(value));
        product.replaceOptionGroups(Set.of(group));
        return product;
    }
}
