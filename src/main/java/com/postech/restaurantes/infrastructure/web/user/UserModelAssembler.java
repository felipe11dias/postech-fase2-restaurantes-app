package com.postech.restaurantes.infrastructure.web.user;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageResult;
import java.util.List;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
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

    /** Preserva os metadados da página do núcleo na representação paginada do Spring HATEOAS. */
    public PagedModel<EntityModel<UserResponse>> toPagedModel(PageResult<UserView> page) {
        List<EntityModel<UserResponse>> conteudo = page.content().stream().map(this::toModel).toList();
        PagedModel.PageMetadata metadados = new PagedModel.PageMetadata(
                page.size(), page.page(), page.totalElements(), page.totalPages());
        return PagedModel.of(conteudo, metadados, usersLink());
    }

    Link selfLink(UserView view) {
        return linkTo(UserRestController.class).slash(view.id()).withSelfRel();
    }

    private Link usersLink() {
        return linkTo(UserRestController.class).withRel(USERS_REL);
    }
}
