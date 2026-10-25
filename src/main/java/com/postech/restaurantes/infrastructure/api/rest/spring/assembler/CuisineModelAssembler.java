package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import com.postech.restaurantes.adapter.presenter.view.CuisineView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.CuisineResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import java.util.List;
import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.BasicLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CuisineModelAssembler {

    static final String CUISINES_REL = "cuisines";

    public EntityModel<CuisineResponse> toModel(CuisineView view) {
        return EntityModel.of(CuisineResponse.from(view), selfLink(view), cuisinesLink());
    }

    public PagedModel<EntityModel<CuisineResponse>> toPagedModel(PageResult<CuisineView> page, String name,
                                                                 String sort) {
        List<EntityModel<CuisineResponse>> conteudo = page.content().stream().map(this::toModel).toList();
        PagedModel.PageMetadata metadados = new PagedModel.PageMetadata(
                page.size(), page.page(), page.totalElements(), page.totalPages());
        PagedModel<EntityModel<CuisineResponse>> model = PagedModel.of(conteudo, metadados);
        model.add(pageLink(page, page.page(), name, sort, IanaLinkRelations.SELF));
        model.add(pageLink(page, 0, name, sort, IanaLinkRelations.FIRST));
        if (page.hasPrevious()) {
            model.add(pageLink(page, page.page() - 1, name, sort, IanaLinkRelations.PREV));
        }
        if (page.hasNext()) {
            model.add(pageLink(page, page.page() + 1, name, sort, IanaLinkRelations.NEXT));
        }
        model.add(pageLink(page, Math.max(page.totalPages() - 1, 0), name, sort, IanaLinkRelations.LAST));
        model.add(cuisinesLink());
        return model;
    }

    public Link selfLink(CuisineView view) {
        return cuisines().slash(view.id()).withSelfRel();
    }

    private static Link pageLink(PageResult<?> page, int number, String name, String sort, LinkRelation rel) {
        String href = UriComponentsBuilder.fromUri(cuisines().toUri())
                .queryParamIfPresent("name", Optional.ofNullable(name))
                .queryParam("page", number)
                .queryParam("size", page.size())
                .queryParamIfPresent("sort", Optional.ofNullable(sort))
                .build()
                .encode()
                .toUriString();
        return Link.of(href, rel);
    }

    private Link cuisinesLink() {
        return cuisines().withRel(CUISINES_REL);
    }

    private static BasicLinkBuilder cuisines() {
        return BasicLinkBuilder.linkToCurrentMapping().slash(ApiRoutes.CUISINES);
    }
}