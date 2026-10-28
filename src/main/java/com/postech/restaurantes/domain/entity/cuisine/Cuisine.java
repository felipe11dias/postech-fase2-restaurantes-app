package com.postech.restaurantes.domain.entity.cuisine;

import com.postech.restaurantes.domain.Guard;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Cuisine {

    private final UUID id;
    private String name;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastUpdatedAt;

    private Cuisine(UUID id, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    public static Cuisine create(String name) {
        Cuisine cuisine = new Cuisine(null, null, null);
        cuisine.setName(name);
        return cuisine;
    }

    public static Cuisine restore(UUID id, String name, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
        Guard.requireNonNull(id, "Id do tipo de cozinha inválido");
        Cuisine cuisine = new Cuisine(id, createdAt, lastUpdatedAt);
        cuisine.setName(name);
        return cuisine;
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome do tipo de cozinha inválido");
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
