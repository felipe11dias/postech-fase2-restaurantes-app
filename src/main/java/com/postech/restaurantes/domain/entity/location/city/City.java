package com.postech.restaurantes.domain.entity.location.city;

import com.postech.restaurantes.domain.Guard;
import java.util.UUID;

/** Cidade pertencente a um estado. */
public final class City {

    private final UUID id;
    private UUID stateId;
    private String name;

    private City(UUID id) {
        this.id = id;
    }

    public static City create(UUID stateId, String name) {
        return fill(new City(null), stateId, name);
    }

    public static City restore(UUID id, UUID stateId, String name) {
        return fill(new City(Guard.requireNonNull(id, "Id da cidade inválido")), stateId, name);
    }

    private static City fill(City city, UUID stateId, String name) {
        city.setStateId(stateId);
        city.setName(name);
        return city;
    }

    public void setStateId(UUID stateId) {
        this.stateId = Guard.requireNonNull(stateId, "Estado da cidade inválido");
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome da cidade inválido");
    }

    public UUID getId() {
        return id;
    }

    public UUID getStateId() {
        return stateId;
    }

    public String getName() {
        return name;
    }
}
