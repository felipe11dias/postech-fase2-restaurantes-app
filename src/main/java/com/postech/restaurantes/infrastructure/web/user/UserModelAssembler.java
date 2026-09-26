package com.postech.restaurantes.infrastructure.web.user;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.List;
import java.util.Optional;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.stereotype.Component;

/**
 * Acrescenta os links de navegação à representação HTTP. É trabalho de <em>canal</em>: HATEOAS
 * é uma característica do REST, não do caso de uso, e por isso não existe rastro dele na
 * {@link UserView}.
 */
@Component
public class UserModelAssembler {

    static final String USERS_REL = "users";

    public EntityModel<UserResponse> toModel(UserView view) {
        return EntityModel.of(UserResponse.from(view), selfLink(view), usersLink());
    }

    /**
     * Preserva os metadados da página do núcleo e dá ao cliente os links para percorrê-la:
     * {@code self}, {@code first} e {@code last} sempre; {@code prev} e {@code next} só quando
     * existe página antes ou depois. O cliente navega seguindo links, sem montar URL.
     *
     * <p>{@code name} e {@code sort} voltam como o cliente os mandou, para que cada link repita
     * a mesma busca. Chegam já decodificados e são codificados uma única vez aqui — reaproveitar
     * a query string crua da requisição codificaria de novo o que já veio codificado.
     */
    public PagedModel<EntityModel<UserResponse>> toPagedModel(PageResult<UserView> page, String name, String sort) {
        List<EntityModel<UserResponse>> conteudo = page.content().stream().map(this::toModel).toList();
        PagedModel.PageMetadata metadados = new PagedModel.PageMetadata(
                page.size(), page.page(), page.totalElements(), page.totalPages());
        PagedModel<EntityModel<UserResponse>> model = PagedModel.of(conteudo, metadados);
        model.add(pageLink(page, page.page(), name, sort, IanaLinkRelations.SELF));
        model.add(pageLink(page, 0, name, sort, IanaLinkRelations.FIRST));
        if (page.hasPrevious()) {
            model.add(pageLink(page, page.page() - 1, name, sort, IanaLinkRelations.PREV));
        }
        if (page.hasNext()) {
            model.add(pageLink(page, page.page() + 1, name, sort, IanaLinkRelations.NEXT));
        }
        model.add(pageLink(page, Math.max(page.totalPages() - 1, 0), name, sort, IanaLinkRelations.LAST));
        model.add(usersLink());
        return model;
    }

    Link selfLink(UserView view) {
        return linkTo(UserRestController.class).slash(view.id()).withSelfRel();
    }

    private static Link pageLink(PageResult<?> page, int number, String name, String sort, LinkRelation rel) {
        String href = linkTo(UserRestController.class).toUriComponentsBuilder()
                .queryParamIfPresent("name", Optional.ofNullable(name))
                .queryParam("page", number)
                .queryParam("size", page.size())
                .queryParamIfPresent("sort", Optional.ofNullable(sort))
                .build()
                .encode()
                .toUriString();
        return Link.of(href, rel);
    }

    private Link usersLink() {
        return linkTo(UserRestController.class).withRel(USERS_REL);
    }
}
