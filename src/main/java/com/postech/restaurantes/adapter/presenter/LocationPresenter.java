package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.CityView;
import com.postech.restaurantes.adapter.presenter.view.StateView;
import com.postech.restaurantes.domain.entity.location.city.City;
import com.postech.restaurantes.domain.entity.location.state.State;
import java.util.Objects;

public final class LocationPresenter {
    private LocationPresenter() {}

    public static StateView toView(State state) {
        return new StateView(state.getId(), state.getName(), state.getAcronym());
    }

    public static CityView toView(City city) {
        var id = Objects.requireNonNull(city.getId(), "Id inválido");
        var state = Objects.requireNonNull(city.getState(), "Estado da cidade inválido");
        return new CityView(id, city.getName(), toView(state));
    }
}
