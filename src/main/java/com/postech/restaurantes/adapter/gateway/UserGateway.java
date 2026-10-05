package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.gateway.mapping.AddressMapping;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.role.Role;
import com.postech.restaurantes.domain.entity.role.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import com.postech.restaurantes.domain.vo.Email;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tradutor entre o agregado {@link User} e o {@link UserData} que a origem de dados entende.
 * Não sabe se a origem é um banco relacional, um serviço ou memória — só conhece a interface.
 */
public final class UserGateway implements IUserGateway {

    private final IUserDataSource dataSource;

    private UserGateway(IUserDataSource dataSource) {
        this.dataSource = Guard.requireNonNull(dataSource, "Origem de dados de usuário inválida");
    }

    public static UserGateway create(IUserDataSource dataSource) {
        return new UserGateway(dataSource);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return dataSource.findById(id).map(UserGateway::toEntity);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return dataSource.findByLogin(login).map(UserGateway::toEntity);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return dataSource.findByEmail(email.value()).map(UserGateway::toEntity);
    }

    @Override
    public PageResult<User> search(String name, PageRequest request) {
        return dataSource.search(name, request).map(UserGateway::toEntity);
    }

    @Override
    public User insert(User user) {
        return toEntity(dataSource.insert(toData(user)));
    }

    @Override
    public User update(User user) {
        return toEntity(dataSource.update(toData(user)));
    }

    @Override
    public void delete(UUID id) {
        dataSource.delete(id);
    }

    static User toEntity(UserData data) {
        return User.restore(data.id(), data.name(), data.email(), data.login(), data.passwordHash(),
                toRoles(data.roles()), toAddresses(data.addresses()), data.createdAt(), data.lastUpdatedAt());
    }

    static UserData toData(User user) {
        return new UserData(user.getId(), user.getName(), user.getEmail().value(), user.getLogin(),
                user.getPasswordHash(),
                user.getRoles().stream().map(UserGateway::toData).collect(Collectors.toCollection(LinkedHashSet::new)),
                user.getAddresses().stream().map(UserGateway::toData).toList(),
                user.getCreatedAt(), user.getLastUpdatedAt());
    }

    private static Set<Role> toRoles(Set<RoleData> roles) {
        return roles.stream()
                .map(role -> Role.restore(role.id(), RoleName.from(role.name())))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static List<UserAddress> toAddresses(List<UserAddressData> addresses) {
        return addresses.stream().map(UserGateway::toEntity).toList();
    }

    private static UserAddress toEntity(UserAddressData data) {
        return UserAddress.restore(data.id(), data.label(), data.isDefault(), AddressMapping.toEntity(data.address()));
    }

    private static RoleData toData(Role role) {
        return new RoleData(role.getId(), role.getName().name());
    }

    private static UserAddressData toData(UserAddress userAddress) {
        return new UserAddressData(userAddress.getId(), userAddress.getLabel(), userAddress.isDefault(),
                AddressMapping.toData(userAddress.getAddress()));
    }
}
