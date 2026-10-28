package com.postech.restaurantes.adapter.presenter.view;

import java.util.Objects;
import java.util.UUID;

public record StateView(UUID id, String name, String acronym) {
    public StateView {
        Objects.requireNonNull(id, "Id inválido");
    }
}
