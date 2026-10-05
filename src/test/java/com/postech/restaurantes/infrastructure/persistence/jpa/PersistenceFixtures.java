package com.postech.restaurantes.infrastructure.persistence.jpa;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.infrastructure.persistence.jpa.address.AddressJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.password.PasswordResetTokenJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.role.RoleJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.UserJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.address.UserAddressJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Dados de apoio dos testes de persistência. */
public final class PersistenceFixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);
    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID ROLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ADDRESS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID TOKEN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    public static final UUID USER_ADDRESS_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    public static final String HASH = "$2a$10$hashDeExemploComTamanhoSuficienteParaBCrypt";

    public static final RoleData CUSTOMER_DATA = new RoleData(ROLE_ID, "ROLE_CUSTOMER");

    public static final AddressData ADDRESS_DATA = new AddressData(ADDRESS_ID, "Rua das Flores", "100", "Apto 21",
            "Centro", "São Paulo", "SP", "01001000");

    public static final UserAddressData USER_ADDRESS_DATA =
            new UserAddressData(USER_ADDRESS_ID, "Casa", true, ADDRESS_DATA);

    public static final UserData USER_DATA = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva",
            HASH, Set.of(CUSTOMER_DATA), List.of(USER_ADDRESS_DATA), NOW.minusDays(1), NOW);

    public static final PasswordResetTokenData TOKEN_DATA = new PasswordResetTokenData(TOKEN_ID, USER_ID,
            "hash-do-token", NOW.plusMinutes(30), false);

    private PersistenceFixtures() {
    }

    public static RoleJpaEntity roleEntity() {
        RoleJpaEntity role = new RoleJpaEntity();
        role.setId(ROLE_ID);
        role.setName("ROLE_CUSTOMER");
        return role;
    }

    public static AddressJpaEntity addressEntity() {
        AddressJpaEntity address = new AddressJpaEntity();
        address.setId(ADDRESS_ID);
        address.setStreet("Rua das Flores");
        address.setNumber("100");
        address.setComplement("Apto 21");
        address.setNeighborhood("Centro");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setZipCode("01001000");
        return address;
    }

    /** Vínculo já persistido do usuário com o endereço da fixture, marcado como padrão. */
    public static UserAddressJpaEntity userAddressEntity() {
        UserAddressJpaEntity userAddress = new UserAddressJpaEntity();
        userAddress.setId(USER_ADDRESS_ID);
        userAddress.setLabel("Casa");
        userAddress.setDefaultAddress(true);
        userAddress.setAddress(addressEntity());
        return userAddress;
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
        user.replaceRoles(Set.of(roleEntity()));
        user.replaceAddresses(List.of(userAddressEntity()));
        return user;
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
