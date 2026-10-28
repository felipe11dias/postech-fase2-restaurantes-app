package com.postech.restaurantes.adapter.gateway.mapping;

import com.postech.restaurantes.adapter.datasource.data.CityData;
import com.postech.restaurantes.adapter.datasource.data.StateData;
import com.postech.restaurantes.domain.entity.location.city.City;
import com.postech.restaurantes.domain.entity.location.state.State;

/** Tradução do catálogo, sem tipos do domínio nos registros da origem de dados. */
public final class LocationMapping {
    private LocationMapping() {}

    public static City toEntity(CityData data) {
        return City.restore(data.id(), toEntity(data.state()), data.name());
    }

    public static State toEntity(StateData data) {
        return State.restore(data.id(), data.name(), data.acronym());
    }

    public static CityData toData(City city) {
        return new CityData(city.getId(), city.getName(), toData(city.getState()));
    }

    public static StateData toData(State state) {
        return new StateData(state.getId(), state.getName(), state.getAcronym());
    }
}
