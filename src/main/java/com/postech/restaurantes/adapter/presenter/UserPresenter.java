package com.postech.restaurantes.adapter.presenter;

import com.postech.restaurantes.adapter.presenter.view.AdminProfileView;
import com.postech.restaurantes.adapter.presenter.view.ClientProfileView;
import com.postech.restaurantes.adapter.presenter.view.CourierProfileView;
import com.postech.restaurantes.adapter.presenter.view.OwnerProfileView;
import com.postech.restaurantes.adapter.presenter.view.UserAddressView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import com.postech.restaurantes.domain.entity.user.UserProfiles;

/**
 * Prepara a saída de usuário. É o único lugar que decide o que <em>não</em> sai: o hash da
 * senha nunca cruza para fora. Papéis, perfis e endereços saem como a entidade os guarda; os
 * documentos, sem máscara.
 */
public final class UserPresenter {

    private UserPresenter() {
    }

    public static UserView toView(User user) {
        Guard.requireNonNull(user, "Usuário inválido");
        UserProfiles profiles = user.getProfiles();
        return new UserView(
                user.getId(),
                user.getName(),
                user.getEmail().value(),
                user.getLogin(),
                user.getRoles().stream().map(RoleName::name).toList(),
                toView(profiles.owner()),
                toView(profiles.client()),
                toView(profiles.courier()),
                toView(profiles.admin()),
                user.getAddresses().stream().map(UserPresenter::toView).toList(),
                user.getCreatedAt(),
                user.getLastUpdatedAt());
    }

    public static PageResult<UserView> toView(PageResult<User> page) {
        Guard.requireNonNull(page, "Página inválida");
        return page.map(UserPresenter::toView);
    }

    private static OwnerProfileView toView(OwnerProfile owner) {
        return owner == null ? null
                : new OwnerProfileView(owner.getCnpj().value(), owner.getLegalName(), owner.getBusinessPhone().value());
    }

    private static ClientProfileView toView(ClientProfile client) {
        return client == null ? null
                : new ClientProfileView(client.getCpf().value(), client.getPhone().value(), client.getBirthDate());
    }

    private static CourierProfileView toView(CourierProfile courier) {
        if (courier == null) {
            return null;
        }
        return new CourierProfileView(courier.getCpf().value(), courier.getPhone().value(),
                courier.getVehicleType().name(),
                courier.getDriverLicense() == null ? null : courier.getDriverLicense().value(),
                courier.getVehiclePlate() == null ? null : courier.getVehiclePlate().value(),
                courier.getStatus().name());
    }

    private static AdminProfileView toView(AdminProfile admin) {
        return admin == null ? null
                : new AdminProfileView(admin.getEmployeeCode(), admin.getDepartment(), admin.isSuperAdmin());
    }

    private static UserAddressView toView(UserAddress userAddress) {
        return new UserAddressView(userAddress.getId(), userAddress.getLabel(), userAddress.isDefault(),
                AddressPresenter.toView(userAddress.getAddress()));
    }
}
