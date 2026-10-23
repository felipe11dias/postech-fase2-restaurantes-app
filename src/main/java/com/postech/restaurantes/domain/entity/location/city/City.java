package com.postech.restaurantes.domain.entity.location.city;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.location.state.State;
import java.util.UUID;

/** Cidade pertencente a um estado. */
public final class City {

    private final UUID id;
    private State state;
    private String name;

    private City(UUID id) {
        this.id = id;
    }

    public static City create(State state, String name) {
        return fill(new City(null), state, name);
    }

    public static City restore(UUID id, State state, String name) {
        return fill(new City(Guard.requireNonNull(id, "Id da cidade inválido")), state, name);
    }

    private static City fill(City city, State state, String name) {
        city.setState(state);
        city.setName(name);
        return city;
    }

    public void setState(State state) {
        Guard.requireNonNull(state, "Estado da cidade inválido");
        Guard.requireNonNull(state.getId(), "Id do estado da cidade inválido");
        this.state = state;
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome da cidade inválido");
    }

    public UUID getId() {
        return id;
    }

    public UUID getStateId() {
        return state.getId();
    }

    public State getState() {
        return state;
    }

    public String getName() {
        return name;
    }
}
