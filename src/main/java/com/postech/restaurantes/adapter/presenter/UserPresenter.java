package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.RoleView;
import com.postech.restaurantes.adapter.presenter.view.UserAddressView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.role.Role;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;

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

    private static UserAddressView toView(UserAddress userAddress) {
        return new UserAddressView(userAddress.getId(), userAddress.getLabel(), userAddress.isDefault(),
                AddressPresenter.toView(userAddress.getAddress()));
    }
}
