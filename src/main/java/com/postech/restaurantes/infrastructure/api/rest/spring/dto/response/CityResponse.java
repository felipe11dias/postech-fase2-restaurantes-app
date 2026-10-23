package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.CityView;
import java.util.UUID;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "cities")
public record CityResponse(UUID id, String name, StateResponse state) {
    public static CityResponse from(CityView view) {
        return new CityResponse(view.id(), view.name(), StateResponse.from(view.state()));
    }
}
