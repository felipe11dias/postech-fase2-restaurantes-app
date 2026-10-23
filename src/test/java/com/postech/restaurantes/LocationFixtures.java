package com.postech.restaurantes;

import com.postech.restaurantes.adapter.datasource.data.CityData;
import com.postech.restaurantes.adapter.datasource.data.StateData;
import com.postech.restaurantes.adapter.gateway.mapping.LocationMapping;
import com.postech.restaurantes.adapter.presenter.LocationPresenter;
import com.postech.restaurantes.adapter.presenter.view.CityView;
import com.postech.restaurantes.domain.entity.location.city.City;
import java.util.UUID;

/** Catálogo compartilhado pelos testes; ids correspondem à seed quando usados por integração. */
public final class LocationFixtures {
    public static final UUID CITY_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    public static final UUID RIO_CITY_ID = UUID.fromString("c0000000-0000-4000-8000-000000000002");
    public static final UUID STATE_ID = UUID.fromString("d0000000-0000-4000-8000-000000000026");
    public static final StateData STATE_DATA = new StateData(STATE_ID, "São Paulo", "SP");
    public static final CityData CITY_DATA = new CityData(CITY_ID, "São Paulo", STATE_DATA);

    private LocationFixtures() {}

    public static UUID cityId(String name, String acronym) {
        return "RJ".equalsIgnoreCase(acronym) ? RIO_CITY_ID : CITY_ID;
    }

    public static CityData cityData(String name, String acronym) {
        return cityData(cityId(name, acronym), name, acronym);
    }

    public static CityData cityData(UUID id, String name, String acronym) {
        String normalized = acronym.toUpperCase();
        StateData state = switch (normalized) {
            case "RJ" -> new StateData(UUID.fromString("d0000000-0000-4000-8000-000000000019"),
                    "Rio de Janeiro", normalized);
            case "MG" -> new StateData(UUID.fromString("d0000000-0000-4000-8000-000000000013"),
                    "Minas Gerais", normalized);
            default -> STATE_DATA;
        };
        return new CityData(id, name, state);
    }

    public static City city(String name, String acronym) {
        return LocationMapping.toEntity(cityData(name, acronym));
    }

    public static City city(UUID id, String name, String acronym) {
        return LocationMapping.toEntity(cityData(id, name, acronym));
    }

    public static CityView cityView(String name, String acronym) {
        return LocationPresenter.toView(city(name, acronym));
    }
}
