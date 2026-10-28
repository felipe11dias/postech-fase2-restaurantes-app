package com.postech.restaurantes.infrastructure.persistence.jpa;

import com.postech.restaurantes.LocationFixtures;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.OfficeHourData;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.infrastructure.persistence.jpa.location.CityJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.location.StateJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.address.AddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.password.PasswordResetTokenJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.UserJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.address.UserAddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.admin.AdminJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.client.ClientJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.courier.CourierJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.owner.OwnerJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Dados de apoio dos testes de persistência. */
public final class PersistenceFixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);
    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID ADDRESS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID STATE_ID = LocationFixtures.STATE_ID;
    public static final UUID CITY_ID = LocationFixtures.CITY_ID;
    public static final UUID TOKEN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID USER_ADDRESS_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final String HASH = "$2a$10$hashDeExemploComTamanhoSuficienteParaBCrypt";

    public static final ClientData CLIENT_DATA = new ClientData("52998224725", "11912345678", LocalDate.of(1990, 5, 20));

    public static final List<OfficeHourData> OFFICE_HOURS_DATA =
            List.of(new OfficeHourData("MONDAY", LocalTime.of(8, 0), LocalTime.of(22, 0)));

    public static final AddressData ADDRESS_DATA = new AddressData(ADDRESS_ID, LocationFixtures.cityId("São Paulo", "SP"), "Rua das Flores", "100", "Apto 21", "Centro", "01001000", LocationFixtures.cityData("São Paulo", "SP"));

    public static final UserAddressData USER_ADDRESS_DATA =
            new UserAddressData(USER_ADDRESS_ID, "Casa", true, ADDRESS_DATA);

    public static final UserData USER_DATA = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva",
            HASH, null, CLIENT_DATA, null, null, List.of(USER_ADDRESS_DATA), NOW.minusDays(1), NOW);

    public static final PasswordResetTokenData TOKEN_DATA = new PasswordResetTokenData(TOKEN_ID, USER_ID,
            "hash-do-token", NOW.plusMinutes(30), false);

    private PersistenceFixtures() {
    }

    public static AddressJpaEntity addressEntity() {
        AddressJpaEntity address = new AddressJpaEntity();
        address.setId(ADDRESS_ID);
        address.setStreet("Rua das Flores");
        address.setNumber("100");
        address.setComplement("Apto 21");
        address.setNeighborhood("Centro");
        address.setCity(cityEntity());
        address.setZipCode("01001000");
        return address;
    }

    public static UserAddressJpaEntity userAddressEntity() {
        UserAddressJpaEntity entity = new UserAddressJpaEntity();
        entity.setId(USER_ADDRESS_ID);
        entity.setLabel("Casa");
        entity.setDefaultAddress(true);
        entity.setAddress(addressEntity());
        return entity;
    }

    public static StateJpaEntity stateEntity() {
        StateJpaEntity state = new StateJpaEntity();
        state.setId(STATE_ID);
        state.setName("São Paulo");
        state.setAcronym("SP");
        return state;
    }

    public static CityJpaEntity cityEntity() {
        CityJpaEntity city = new CityJpaEntity();
        city.setId(CITY_ID);
        city.setName("São Paulo");
        city.setState(stateEntity());
        return city;
    }

    /** Usuário já persistido: com id, auditoria, um papel e um endereço. */
    public static UserJpaEntity userEntity() {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(USER_ID);
        user.setName("João Silva");
        user.setEmail("joao.silva@email.com");
        user.setLogin("joao.silva");
        user.setPassword(HASH);
        user.auditadaEm(NOW.minusDays(1), NOW);
        user.setClient(clientEntity());
        user.replaceAddresses(List.of(userAddressEntity()));
        return user;
    }

    public static OwnerJpaEntity ownerEntity() {
        OwnerJpaEntity owner = new OwnerJpaEntity();
        owner.setId(USER_ID);
        owner.setCnpj("11222333000181");
        owner.setLegalName("Sabor Ltda");
        owner.setBusinessPhone("1131234567");
        return owner;
    }

    public static ClientJpaEntity clientEntity() {
        ClientJpaEntity client = new ClientJpaEntity();
        client.setId(USER_ID);
        client.setCpf("52998224725");
        client.setPhone("11912345678");
        client.setBirthDate(LocalDate.of(1990, 5, 20));
        return client;
    }

    public static CourierJpaEntity courierEntity() {
        CourierJpaEntity courier = new CourierJpaEntity();
        courier.setId(USER_ID);
        courier.setCpf("52998224725");
        courier.setPhone("11912345678");
        courier.setVehicleType("MOTORCYCLE");
        courier.setDriverLicense("02650306461");
        courier.setVehiclePlate("ABC1D23");
        courier.setStatus("AVAILABLE");
        return courier;
    }

    public static AdminJpaEntity adminEntity() {
        AdminJpaEntity admin = new AdminJpaEntity();
        admin.setId(USER_ID);
        admin.setEmployeeCode("ADM-1");
        admin.setDepartment("Operações");
        admin.setSuperAdmin(true);
        return admin;
    }

    public static PasswordResetTokenJpaEntity tokenEntity() {
        PasswordResetTokenJpaEntity token = new PasswordResetTokenJpaEntity();
        token.setId(TOKEN_ID);
        token.setUserId(USER_ID);
        token.setTokenHash("hash-do-token");
        token.setExpiresAt(NOW.plusMinutes(30));
        token.setUsed(false);
        return token;
    }
}
