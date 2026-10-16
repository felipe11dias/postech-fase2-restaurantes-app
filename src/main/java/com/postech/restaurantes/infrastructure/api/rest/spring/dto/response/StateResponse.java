package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.StateView;
import java.util.UUID;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "states")
public record StateResponse(UUID id, String name, String acronym) {
    public static StateResponse from(StateView view) {
        return new StateResponse(view.id(), view.name(), view.acronym());
    }
}
