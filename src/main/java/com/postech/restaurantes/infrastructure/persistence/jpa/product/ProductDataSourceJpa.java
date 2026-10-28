package com.postech.restaurantes.infrastructure.persistence.jpa.product;

import com.postech.restaurantes.adapter.datasource.IProductDataSource;
import com.postech.restaurantes.adapter.datasource.data.ProductData;
import com.postech.restaurantes.adapter.datasource.data.ProductOptionGroupData;
import com.postech.restaurantes.adapter.datasource.data.ProductOptionValueData;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.image.ProductImageJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.optiongroup.ProductOptionGroupJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.optionvalue.ProductOptionValueJpaEntity;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Implementação JPA da origem de dados de produtos. */
@Repository
public class ProductDataSourceJpa implements IProductDataSource {

    private final SpringDataProductRepository products;

    public ProductDataSourceJpa(SpringDataProductRepository products) {
        this.products = products;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductData> findById(UUID restaurantId, UUID productId) {
        return products.findByIdAndRestaurantId(productId, restaurantId).map(ProductDataSourceJpa::toData);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ProductData> search(UUID restaurantId, PageRequest request) {
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        Pageable pageable = org.springframework.data.domain.PageRequest.of(request.page(), request.size(), sort);
        Page<UUID> ids = products.findIdsByRestaurantId(restaurantId, pageable);
        List<ProductData> content = ids.isEmpty() ? List.of()
                : products.findByIdIn(ids.getContent(), sort).stream().map(ProductDataSourceJpa::toData).toList();
        return new PageResult<>(content, request.page(), request.size(), ids.getTotalElements());
    }

    @Override
    @Transactional
    public ProductData insert(ProductData data) {
        ProductJpaEntity entity = new ProductJpaEntity();
        apply(entity, data);
        return toData(products.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public ProductData update(ProductData data) {
        ProductJpaEntity entity = products.findByIdAndRestaurantId(data.id(), data.restaurantId())
                .orElseThrow(() -> new IllegalStateException("Produto inexistente para atualização: " + data.id()));
        apply(entity, data);
        return toData(products.saveAndFlush(entity));
    }

    @Override
    @Transactional
    public void delete(UUID restaurantId, UUID productId) {
        products.findByIdAndRestaurantId(productId, restaurantId).ifPresent(products::delete);
    }

    private static void apply(ProductJpaEntity entity, ProductData data) {
        entity.setRestaurantId(data.restaurantId());
        entity.setName(data.name());
        entity.setDescription(data.description());
        entity.setPrice(data.price());
        entity.setDineInOnly(data.dineInOnly());
        var images = data.imagePaths().stream().map(content -> {
            ProductImageJpaEntity image = new ProductImageJpaEntity();
            image.setContent(content);
            return image;
        }).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        entity.replaceImages(images);
        var groups = data.optionGroups().stream().map(group -> {
            ProductOptionGroupJpaEntity optionGroup = new ProductOptionGroupJpaEntity();
            optionGroup.setName(group.name());
            optionGroup.setRequired(group.required());
            optionGroup.setMinSelections(group.minSelections());
            optionGroup.setMaxSelections(group.maxSelections());
            var values = group.values().stream().map(value -> {
                ProductOptionValueJpaEntity optionValue = new ProductOptionValueJpaEntity();
                optionValue.setName(value.name());
                optionValue.setAdditionalPrice(value.additionalPrice());
                return optionValue;
            }).collect(java.util.stream.Collectors.toCollection(HashSet::new));
            optionGroup.replaceValues(values);
            return optionGroup;
        }).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        entity.replaceOptionGroups(groups);
    }

    static ProductData toData(ProductJpaEntity entity) {
        List<String> images = entity.getImages().stream().map(ProductImageJpaEntity::getContent)
                .sorted().toList();
        List<ProductOptionGroupData> groups = entity.getOptionGroups().stream()
                .map(group -> new ProductOptionGroupData(group.getName(), group.isRequired(),
                        group.getMinSelections(), group.getMaxSelections(),
                        group.getValues().stream().map(value ->
                                new ProductOptionValueData(value.getName(), value.getAdditionalPrice()))
                                .sorted(Comparator.comparing(ProductOptionValueData::name)).toList()))
                .sorted(Comparator.comparing(ProductOptionGroupData::name)).toList();
        return new ProductData(entity.getId(), entity.getRestaurantId(), entity.getName(), entity.getDescription(),
                entity.getPrice(), entity.isDineInOnly(), images, groups);
    }
}
