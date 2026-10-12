package com.postech.restaurantes.domain.entity.location.state;

import com.postech.restaurantes.domain.Guard;
import java.util.UUID;

/** Estado brasileiro identificado por seu nome e sua sigla. */
public final class State {

    private static final int ACRONYM_LENGTH = 2;

    private final UUID id;
    private String name;
    private String acronym;

    private State(UUID id) {
        this.id = id;
    }

    public static State create(String name, String acronym) {
        return fill(new State(null), name, acronym);
    }

    public static State restore(UUID id, String name, String acronym) {
        return fill(new State(Guard.requireNonNull(id, "Id do estado inválido")), name, acronym);
    }

    private static State fill(State state, String name, String acronym) {
        state.setName(name);
        state.setAcronym(acronym);
        return state;
    }

    public void setName(String name) {
        this.name = Guard.requireNonBlank(name, "Nome do estado inválido");
    }

    public void setAcronym(String acronym) {
        String normalized = Guard.requireNonBlank(acronym, "Sigla do estado inválida").toUpperCase();
        Guard.require(normalized.length() == ACRONYM_LENGTH && normalized.chars().allMatch(Character::isLetter),
                "Sigla do estado deve ter 2 letras");
        this.acronym = normalized;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAcronym() {
        return acronym;
    }
}
