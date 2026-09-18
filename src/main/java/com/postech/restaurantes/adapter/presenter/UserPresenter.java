package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import com.postech.restaurantes.adapter.presenter.view.RoleView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.User;

/**
 * Prepara a saída de usuário. É o único lugar que decide o que <em>não</em> sai: o hash da
 * senha nunca cruza para fora. Papéis e endereços saem na ordem em que a entidade os guarda.
 */
public final class UserPresenter {

    private UserPresenter() {
    }

    public static UserView toView(User user) {
        Guard.requireNonNull(user, "Usuário inválido");
        return new UserView(
                user.getId(),
                user.getName(),
                user.getEmail().value(),
                user.getLogin(),
                user.getRoles().stream().map(UserPresenter::toView).toList(),
                user.getAddresses().stream().map(UserPresenter::toView).toList(),
                user.getCreatedAt(),
                user.getLastUpdatedAt());
    }

    public static PageResult<UserView> toView(PageResult<User> page) {
        Guard.requireNonNull(page, "Página inválida");
        return page.map(UserPresenter::toView);
    }

    private static RoleView toView(Role role) {
        return new RoleView(role.getId(), role.getName().name());
    }

    private static AddressView toView(Address address) {
        return new AddressView(address.getId(), address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getCity(), address.getState(), address.getZipCode().value());
    }
}
