package com.postech.restaurantes.infrastructure.persistence.jpa.location;

import com.postech.restaurantes.adapter.datasource.data.CityData;
import com.postech.restaurantes.adapter.datasource.data.StateData;

public final class LocationJpaMapping {
    private LocationJpaMapping() {}

    public static CityData toData(CityJpaEntity city) {
        return new CityData(city.getId(), city.getName(), toData(city.getState()));
    }

    public static StateData toData(StateJpaEntity state) {
        return new StateData(state.getId(), state.getName(), state.getAcronym());
    }
}
