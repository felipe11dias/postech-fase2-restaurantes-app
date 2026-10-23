package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import com.postech.restaurantes.adapter.presenter.view.CityView;
import com.postech.restaurantes.adapter.presenter.view.StateView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.CityResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.StateResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.BasicLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class LocationModelAssembler {
    public CollectionModel<StateResponse> toModel(List<StateView> states) {
        return CollectionModel.of(states.stream().map(StateResponse::from).toList(),
                BasicLinkBuilder.linkToCurrentMapping().slash(ApiRoutes.STATES).withSelfRel());
    }

    public PagedModel<CityResponse> toPagedModel(PageResult<CityView> page, UUID stateId, String name) {
        var metadata = new PagedModel.PageMetadata(page.size(), page.page(), page.totalElements(), page.totalPages());
        var model = PagedModel.of(page.content().stream().map(CityResponse::from).toList(), metadata);
        model.add(pageLink(page, page.page(), stateId, name, "self"));
        model.add(pageLink(page, 0, stateId, name, "first"));
        model.add(pageLink(page, Math.max(page.totalPages() - 1, 0), stateId, name, "last"));
        if (page.hasPrevious()) {
            model.add(pageLink(page, page.page() - 1, stateId, name, "prev"));
        }
        if (page.hasNext()) {
            model.add(pageLink(page, page.page() + 1, stateId, name, "next"));
        }
        return model;
    }

    private static Link pageLink(PageResult<?> page, int number, UUID stateId, String name, String relation) {
        String href = UriComponentsBuilder.fromUri(BasicLinkBuilder.linkToCurrentMapping()
                        .slash(ApiRoutes.CITIES).toUri())
                .queryParamIfPresent("stateId", Optional.ofNullable(stateId))
                .queryParamIfPresent("name", Optional.ofNullable(name))
                .queryParam("page", number).queryParam("size", page.size())
                .build().encode().toUriString();
        return Link.of(href, relation);
    }
}
