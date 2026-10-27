package com.postech.restaurantes.domain.entity.product;

import com.postech.restaurantes.domain.Guard;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Raiz do agregado de produto, incluindo referências de imagem e grupos de opções. */
public final class Product {

    private final UUID id;
    private final UUID restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private boolean dineInOnly;
    private List<String> imagePaths;
    private List<ProductOptionGroup> optionGroups;
    private Product(UUID id, UUID restaurantId) {
        this.id = id;
        this.restaurantId = restaurantId;
    }

    public static Product create(UUID restaurantId, String name, String description, BigDecimal price,
                                 boolean dineInOnly, List<String> imagePaths,
                                 List<ProductOptionGroup> optionGroups) {
        return fill(new Product(null, restaurantId), name, description, price, dineInOnly,
                imagePaths, optionGroups);
    }

    public static Product restore(UUID id, UUID restaurantId, String name, String description, BigDecimal price,
                                  boolean dineInOnly, List<String> imagePaths,
                                  List<ProductOptionGroup> optionGroups) {
        Product product = new Product(Guard.requireNonNull(id, "Id do produto inválido"), restaurantId);
        return fill(product, name, description, price, dineInOnly, imagePaths, optionGroups);
    }

    private static Product fill(Product product, String name, String description, BigDecimal price,
                                boolean dineInOnly, List<String> imagePaths,
                                List<ProductOptionGroup> optionGroups) {
        Guard.requireNonNull(product.restaurantId, "Id do restaurante inválido");
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setDineInOnly(dineInOnly);
        product.replaceImagePaths(imagePaths);
        product.replaceOptionGroups(optionGroups);
        return product;
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome do produto inválido");
    }

    public void setDescription(String description) {
        this.description = Guard.trimToNull(description);
    }

    public void setPrice(BigDecimal price) {
        Guard.requireNonNull(price, "Preço do produto inválido");
        Guard.require(price.signum() >= 0 && price.scale() <= 2
                        && price.precision() - price.scale() <= 8,
                "Preço do produto inválido");
        this.price = price;
    }

    public void setDineInOnly(boolean dineInOnly) {
        this.dineInOnly = dineInOnly;
    }

    public void replaceImagePaths(List<String> imagePaths) {
        Guard.requireNonNull(imagePaths, "Imagens do produto inválidas");
        Guard.require(imagePaths.stream().noneMatch(Objects::isNull), "Caminho de imagem inválido");
        this.imagePaths = imagePaths.stream().map(path -> {
            String normalized = Guard.requireNonBlank(path, "Caminho de imagem inválido");
            Guard.require(normalized.length() <= 2048, "Caminho de imagem inválido");
            return normalized;
        }).toList();
    }

    public void replaceOptionGroups(List<ProductOptionGroup> optionGroups) {
        Guard.requireNonNull(optionGroups, "Grupos de opções inválidos");
        Guard.require(optionGroups.stream().noneMatch(Objects::isNull), "Grupo de opções inválido");
        this.optionGroups = List.copyOf(optionGroups);
    }

    public UUID getId() {
        return id;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isDineInOnly() {
        return dineInOnly;
    }

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public List<ProductOptionGroup> getOptionGroups() {
        return optionGroups;
    }

}
