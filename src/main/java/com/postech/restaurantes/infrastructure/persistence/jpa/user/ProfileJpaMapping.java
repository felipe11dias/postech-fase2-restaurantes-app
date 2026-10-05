package com.postech.restaurantes.infrastructure.persistence.jpa.user;

import com.postech.restaurantes.adapter.datasource.data.AdminData;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.OwnerData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.admin.AdminJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.client.ClientJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.courier.CourierJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.owner.OwnerJpaEntity;
import java.util.function.Supplier;

/**
 * Perfis do usuário: registro do adaptador {@literal <->} entidades JPA. Cada perfil presente no
 * registro é gravado na linha que o usuário já tem (mesmo id) ou numa nova, com o id do usuário; o
 * ausente é removido ({@code orphanRemoval}). A auditoria nunca vem do registro.
 */
final class ProfileJpaMapping {

    private ProfileJpaMapping() {
    }

    /** Exige o usuário já com id: é o id de cada perfil (chave primária compartilhada). */
    static void apply(UserJpaEntity user, UserData data) {
        user.setOwner(data.owner() == null ? null
                : owner(orNew(user.getOwner(), OwnerJpaEntity::new), user, data.owner()));
        user.setClient(data.client() == null ? null
                : client(orNew(user.getClient(), ClientJpaEntity::new), user, data.client()));
        user.setCourier(data.courier() == null ? null
                : courier(orNew(user.getCourier(), CourierJpaEntity::new), user, data.courier()));
        user.setAdmin(data.admin() == null ? null
                : admin(orNew(user.getAdmin(), AdminJpaEntity::new), user, data.admin()));
    }

    /** A linha que o usuário já tem, ou uma nova. */
    private static <T> T orNew(T current, Supplier<T> factory) {
        return current != null ? current : factory.get();
    }

    private static OwnerJpaEntity owner(OwnerJpaEntity entity, UserJpaEntity user, OwnerData data) {
        entity.setId(user.getId());
        entity.setCnpj(data.cnpj());
        entity.setLegalName(data.legalName());
        entity.setBusinessPhone(data.businessPhone());
        return entity;
    }

    private static ClientJpaEntity client(ClientJpaEntity entity, UserJpaEntity user, ClientData data) {
        entity.setId(user.getId());
        entity.setCpf(data.cpf());
        entity.setPhone(data.phone());
        entity.setBirthDate(data.birthDate());
        return entity;
    }

    private static CourierJpaEntity courier(CourierJpaEntity entity, UserJpaEntity user, CourierData data) {
        entity.setId(user.getId());
        entity.setCpf(data.cpf());
        entity.setPhone(data.phone());
        entity.setVehicleType(data.vehicleType());
        entity.setDriverLicense(data.driverLicense());
        entity.setVehiclePlate(data.vehiclePlate());
        entity.setStatus(data.status());
        return entity;
    }

    private static AdminJpaEntity admin(AdminJpaEntity entity, UserJpaEntity user, AdminData data) {
        entity.setId(user.getId());
        entity.setEmployeeCode(data.employeeCode());
        entity.setDepartment(data.department());
        entity.setSuperAdmin(data.superAdmin());
        return entity;
    }

    static OwnerData toData(OwnerJpaEntity entity) {
        return entity == null ? null
                : new OwnerData(entity.getCnpj(), entity.getLegalName(), entity.getBusinessPhone());
    }

    static ClientData toData(ClientJpaEntity entity) {
        return entity == null ? null : new ClientData(entity.getCpf(), entity.getPhone(), entity.getBirthDate());
    }

    static CourierData toData(CourierJpaEntity entity) {
        return entity == null ? null : new CourierData(entity.getCpf(), entity.getPhone(), entity.getVehicleType(),
                entity.getDriverLicense(), entity.getVehiclePlate(), entity.getStatus());
    }

    static AdminData toData(AdminJpaEntity entity) {
        return entity == null ? null
                : new AdminData(entity.getEmployeeCode(), entity.getDepartment(), entity.isSuperAdmin());
    }
}
