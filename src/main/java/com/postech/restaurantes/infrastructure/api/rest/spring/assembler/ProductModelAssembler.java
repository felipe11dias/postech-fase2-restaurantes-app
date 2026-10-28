package com.postech.restaurantes.infrastructure.api.rest.spring.assembler;

import com.postech.restaurantes.adapter.presenter.view.ProductView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.response.ProductResponse;
import com.postech.restaurantes.infrastructure.api.rest.spring.route.ApiRoutes;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.mvc.BasicLinkBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Assembler HATEOAS de produtos. */
@Component
public class ProductModelAssembler {

    public EntityModel<ProductResponse> toModel(ProductView view) {
        return EntityModel.of(ProductResponse.from(view), selfLink(view),
                productsLink(view.restaurantId()));
    }

    public PagedModel<EntityModel<ProductResponse>> toPagedModel(PageResult<ProductView> page,
                                                                  java.util.UUID restaurantId) {
        List<EntityModel<ProductResponse>> content = page.content().stream().map(this::toModel).toList();
        PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                page.size(), page.page(), page.totalElements(), page.totalPages());
        PagedModel<EntityModel<ProductResponse>> model = PagedModel.of(content, metadata);
        model.add(pageLink(restaurantId, page, page.page(), IanaLinkRelations.SELF));
        model.add(pageLink(restaurantId, page, 0, IanaLinkRelations.FIRST));
        if (page.hasPrevious()) {
            model.add(pageLink(restaurantId, page, page.page() - 1, IanaLinkRelations.PREV));
        }
        if (page.hasNext()) {
            model.add(pageLink(restaurantId, page, page.page() + 1, IanaLinkRelations.NEXT));
        }
        model.add(pageLink(restaurantId, page, Math.max(page.totalPages() - 1, 0), IanaLinkRelations.LAST));
        return model;
    }

    public Link selfLink(ProductView view) {
        return productPath(view.restaurantId()).slash(view.id()).withSelfRel();
    }

    private static Link pageLink(java.util.UUID restaurantId, PageResult<?> page, int number, LinkRelation relation) {
        String href = UriComponentsBuilder.fromUri(productsPath(restaurantId).toUri())
                .queryParam("page", number)
                .queryParam("size", page.size())
                .build()
                .encode()
                .toUriString();
        return Link.of(href, relation);
    }

    private Link productsLink(java.util.UUID restaurantId) {
        return productsPath(restaurantId).withRel("products");
    }

    private static BasicLinkBuilder productsPath(java.util.UUID restaurantId) {
        return BasicLinkBuilder.linkToCurrentMapping().slash(ApiRoutes.RESTAURANTS)
                .slash(restaurantId).slash("products");
    }

    private static BasicLinkBuilder productPath(java.util.UUID restaurantId) {
        return productsPath(restaurantId);
    }
}
