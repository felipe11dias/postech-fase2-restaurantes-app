package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import com.postech.restaurantes.adapter.presenter.view.RestaurantView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.RestaurantResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.BasicLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** HATEOAS assembler para representações HTTP de restaurante. */
@Component
public class RestaurantModelAssembler {

    static final String RESTAURANTS_REL = "restaurants";

    public EntityModel<RestaurantResponse> toModel(RestaurantView view) {
        return EntityModel.of(RestaurantResponse.from(view), selfLink(view), restaurantsLink());
    }

    public PagedModel<EntityModel<RestaurantResponse>> toPagedModel(PageResult<RestaurantView> page, String name,
                                                                    UUID ownerId, String sort) {
        List<EntityModel<RestaurantResponse>> conteudo = page.content().stream().map(this::toModel).toList();
        PagedModel.PageMetadata metadados = new PagedModel.PageMetadata(
                page.size(), page.page(), page.totalElements(), page.totalPages());
        PagedModel<EntityModel<RestaurantResponse>> model = PagedModel.of(conteudo, metadados);
        model.add(pageLink(page, page.page(), name, ownerId, sort, IanaLinkRelations.SELF));
        model.add(pageLink(page, 0, name, ownerId, sort, IanaLinkRelations.FIRST));
        if (page.hasPrevious()) {
            model.add(pageLink(page, page.page() - 1, name, ownerId, sort, IanaLinkRelations.PREV));
        }
        if (page.hasNext()) {
            model.add(pageLink(page, page.page() + 1, name, ownerId, sort, IanaLinkRelations.NEXT));
        }
        model.add(pageLink(page, Math.max(page.totalPages() - 1, 0), name, ownerId, sort, IanaLinkRelations.LAST));
        model.add(restaurantsLink());
        return model;
    }

    public Link selfLink(RestaurantView view) {
        return restaurants().slash(view.id()).withSelfRel();
    }

    private static Link pageLink(PageResult<?> page, int number, String name, UUID ownerId, String sort,
                                 LinkRelation rel) {
        String href = UriComponentsBuilder.fromUri(restaurants().toUri())
                .queryParamIfPresent("name", Optional.ofNullable(name))
                .queryParamIfPresent("ownerId", Optional.ofNullable(ownerId))
                .queryParam("page", number)
                .queryParam("size", page.size())
                .queryParamIfPresent("sort", Optional.ofNullable(sort))
                .build()
                .encode()
                .toUriString();
        return Link.of(href, rel);
    }

    private Link restaurantsLink() {
        return restaurants().withRel(RESTAURANTS_REL);
    }

    private static BasicLinkBuilder restaurants() {
        return BasicLinkBuilder.linkToCurrentMapping().slash(ApiRoutes.RESTAURANTS);
    }
}
