package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.datasource.IUserDataSource;
import com.postech.restaurantes.adapter.datasource.data.AdminData;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.OwnerData;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.adapter.gateway.mapping.AddressMapping;
import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.application.gateway.IUserGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierStatus;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import com.postech.restaurantes.domain.vo.Cnpj;
import com.postech.restaurantes.domain.vo.Cpf;
import com.postech.restaurantes.domain.vo.Email;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<User> findByCpf(Cpf cpf) {
        return dataSource.findByCpf(cpf.value()).map(UserGateway::toEntity);
    }

    @Override
    public Optional<User> findByCnpj(Cnpj cnpj) {
        return dataSource.findByCnpj(cnpj.value()).map(UserGateway::toEntity);
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
                toProfiles(data), toAddresses(data.addresses()), data.createdAt(), data.lastUpdatedAt());
    }

    static UserData toData(User user) {
        UserProfiles profiles = user.getProfiles();
        return new UserData(user.getId(), user.getName(), user.getEmail().value(), user.getLogin(),
                user.getPasswordHash(), toData(profiles.owner()), toData(profiles.client()),
                toData(profiles.courier()), toData(profiles.admin()),
                user.getAddresses().stream().map(UserGateway::toData).toList(),
                user.getCreatedAt(), user.getLastUpdatedAt());
    }

    /** Reconstrói com {@code restore}: o que vem da origem de dados passa de novo pelas invariantes. */
    private static UserProfiles toProfiles(UserData data) {
        OwnerData owner = data.owner();
        ClientData client = data.client();
        CourierData courier = data.courier();
        AdminData admin = data.admin();
        return new UserProfiles(
                owner == null ? null : OwnerProfile.restore(owner.cnpj(), owner.legalName(), owner.businessPhone()),
                client == null ? null : ClientProfile.restore(client.cpf(), client.phone(), client.birthDate()),
                courier == null ? null : CourierProfile.restore(courier.cpf(), courier.phone(),
                        CourierVehicleType.from(courier.vehicleType()), courier.driverLicense(),
                        courier.vehiclePlate(), CourierStatus.from(courier.status())),
                admin == null ? null : AdminProfile.restore(admin.employeeCode(), admin.department(),
                        admin.superAdmin()));
    }

    private static OwnerData toData(OwnerProfile owner) {
        return owner == null ? null
                : new OwnerData(owner.getCnpj().value(), owner.getLegalName(), owner.getBusinessPhone().value());
    }

    private static ClientData toData(ClientProfile client) {
        return client == null ? null
                : new ClientData(client.getCpf().value(), client.getPhone().value(), client.getBirthDate());
    }

    private static CourierData toData(CourierProfile courier) {
        if (courier == null) {
            return null;
        }
        return new CourierData(courier.getCpf().value(), courier.getPhone().value(), courier.getVehicleType().name(),
                courier.getDriverLicense() == null ? null : courier.getDriverLicense().value(),
                courier.getVehiclePlate() == null ? null : courier.getVehiclePlate().value(),
                courier.getStatus().name());
    }

    private static AdminData toData(AdminProfile admin) {
        return admin == null ? null
                : new AdminData(admin.getEmployeeCode(), admin.getDepartment(), admin.isSuperAdmin());
    }

    private static List<UserAddress> toAddresses(List<UserAddressData> addresses) {
        return addresses.stream().map(UserGateway::toEntity).toList();
    }

    private static UserAddress toEntity(UserAddressData data) {
        return UserAddress.restore(data.id(), data.label(), data.isDefault(), AddressMapping.toEntity(data.address()));
    }

    private static UserAddressData toData(UserAddress userAddress) {
        return new UserAddressData(userAddress.getId(), userAddress.getLabel(), userAddress.isDefault(),
                AddressMapping.toData(userAddress.getAddress()));
    }
}
