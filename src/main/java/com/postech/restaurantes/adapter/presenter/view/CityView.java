package com.postech.restaurantes.adapter.presenter.view;

import java.util.Objects;
import java.util.UUID;

public record CityView(UUID id, String name, StateView state) {
    public CityView {
        Objects.requireNonNull(id, "Id inválido");
        Objects.requireNonNull(state, "Estado da cidade inválido");
    }
}
