package com.postech.restaurantes.infrastructure.persistence.jpa.user;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.HASH;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.NOW;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.adminEntity;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.clientEntity;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.courierEntity;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ownerEntity;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.userEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.adapter.datasource.data.AdminData;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.OwnerData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.admin.AdminJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.client.ClientJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.courier.CourierJpaEntity;
import com.postech.restaurantes.infrastructure.persistence.jpa.user.owner.OwnerJpaEntity;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Perfis: registro do adaptador ↔ entidades JPA, sem banco. */
class ProfileJpaMappingTest {

    private static final OwnerData OWNER = new OwnerData("11222333000181", "Sabor Ltda", "1131234567");
    private static final ClientData CLIENT = new ClientData("52998224725", "11912345678", LocalDate.of(1990, 5, 20));
    private static final CourierData COURIER =
            new CourierData("52998224725", "11912345678", "CAR", "02650306461", "ABC1D23", "BUSY");
    private static final AdminData ADMIN = new AdminData("ADM-1", "Operações", true);

    private static UserData comPerfis(OwnerData owner, ClientData client, CourierData courier, AdminData admin) {
        return new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH, owner, client,
                courier, admin, List.of(), NOW, NOW);
    }

    @Test
    @DisplayName("Perfil novo nasce com o id do usuário e os campos do registro")
    void deveCriarOsPerfisAusentesComOIdDoUsuario() {
        UserJpaEntity user = userEntity();
        user.setClient(null);

        ProfileJpaMapping.apply(user, comPerfis(OWNER, CLIENT, COURIER, ADMIN));

        assertEquals(USER_ID, user.getOwner().getId());
        assertEquals("Sabor Ltda", user.getOwner().getLegalName());
        assertEquals(USER_ID, user.getClient().getId());
        assertEquals(LocalDate.of(1990, 5, 20), user.getClient().getBirthDate());
        assertEquals(USER_ID, user.getCourier().getId());
        assertEquals("CAR", user.getCourier().getVehicleType());
        assertEquals("BUSY", user.getCourier().getStatus());
        assertEquals(USER_ID, user.getAdmin().getId());
        assertTrue(user.getAdmin().isSuperAdmin());
    }

    @Test
    @DisplayName("Perfil que o usuário já tem é atualizado na mesma linha")
    void deveAtualizarOPerfilExistenteNaMesmaLinha() {
        UserJpaEntity user = userEntity();
        ClientJpaEntity existente = user.getClient();
        user.setOwner(ownerEntity());
        user.setCourier(courierEntity());
        user.setAdmin(adminEntity());

        ProfileJpaMapping.apply(user, comPerfis(OWNER, new ClientData("52998224725", "1133334444", null), COURIER,
                ADMIN));

        assertSame(existente, user.getClient());
        assertEquals("1133334444", existente.getPhone());
        assertNull(existente.getBirthDate());
    }

    @Test
    @DisplayName("Perfil ausente no registro sai do usuário (orphanRemoval apaga a linha)")
    void deveRemoverOsPerfisAusentes() {
        UserJpaEntity user = userEntity();
        user.setOwner(ownerEntity());
        user.setCourier(courierEntity());
        user.setAdmin(adminEntity());

        ProfileJpaMapping.apply(user, comPerfis(null, null, null, ADMIN));

        assertNull(user.getOwner());
        assertNull(user.getClient());
        assertNull(user.getCourier());
        assertEquals("ADM-1", user.getAdmin().getEmployeeCode());
    }

    @Test
    @DisplayName("Perfil novo de entregador a pé vai sem CNH nem placa")
    void deveCriarEntregadorSemDocumentosDeVeiculo() {
        UserJpaEntity user = userEntity();

        ProfileJpaMapping.apply(user, comPerfis(null, CLIENT,
                new CourierData("52998224725", "11912345678", "ON_FOOT", null, null, "OFFLINE"), null));

        assertNull(user.getCourier().getDriverLicense());
        assertNull(user.getCourier().getVehiclePlate());
        assertNotNull(user.getClient());
    }

    @Test
    @DisplayName("Cada perfil volta da entidade para o registro sem perda; ausente volta nulo")
    void deveTraduzirParaORegistro() {
        assertEquals(OWNER, ProfileJpaMapping.toData(ownerEntity()));
        assertEquals(CLIENT, ProfileJpaMapping.toData(clientEntity()));
        assertEquals(new CourierData("52998224725", "11912345678", "MOTORCYCLE", "02650306461", "ABC1D23",
                "AVAILABLE"), ProfileJpaMapping.toData(courierEntity()));
        assertEquals(ADMIN, ProfileJpaMapping.toData(adminEntity()));
        assertNull(ProfileJpaMapping.toData((OwnerJpaEntity) null));
        assertNull(ProfileJpaMapping.toData((ClientJpaEntity) null));
        assertNull(ProfileJpaMapping.toData((CourierJpaEntity) null));
        assertNull(ProfileJpaMapping.toData((AdminJpaEntity) null));
    }
}
