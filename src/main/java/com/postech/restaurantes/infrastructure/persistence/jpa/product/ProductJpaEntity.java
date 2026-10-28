package com.postech.restaurantes.infrastructure.persistence.jpa.product;

import com.postech.restaurantes.infrastructure.persistence.jpa.product.image.ProductImageJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.product.optiongroup.ProductOptionGroupJpaEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Mapeamento JPA separado da entidade de domínio. */
@Entity
@Table(name = "products")
public class ProductJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false, updatable = false)
    private UUID restaurantId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_dine_in_only", nullable = false)
    private boolean dineInOnly;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Set<ProductImageJpaEntity> images = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Set<ProductOptionGroupJpaEntity> optionGroups = new HashSet<>();

    public UUID getId() {
        return id;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isDineInOnly() {
        return dineInOnly;
    }

    public void setDineInOnly(boolean dineInOnly) {
        this.dineInOnly = dineInOnly;
    }

    public Set<ProductImageJpaEntity> getImages() {
        return images;
    }

    public void replaceImages(Set<ProductImageJpaEntity> images) {
        this.images.clear();
        this.images.addAll(images);
    }

    public Set<ProductOptionGroupJpaEntity> getOptionGroups() {
        return optionGroups;
    }

    public void replaceOptionGroups(Set<ProductOptionGroupJpaEntity> optionGroups) {
        this.optionGroups.clear();
        this.optionGroups.addAll(optionGroups);
    }
}
