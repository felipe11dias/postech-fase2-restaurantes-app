package com.postech.restaurantes.infrastructure.persistence.jpa.user.courier;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.courierEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourierJpaEntityTest {

    @Test
    @DisplayName("Guarda o perfil de entregador; tipo de veículo e status pelo nome do valor do enum do banco")
    void deveGuardarOsCampos() {
        CourierJpaEntity courier = courierEntity();

        assertEquals(USER_ID, courier.getId());
        assertEquals("52998224725", courier.getCpf());
        assertEquals("11912345678", courier.getPhone());
        assertEquals("MOTORCYCLE", courier.getVehicleType());
        assertEquals("02650306461", courier.getDriverLicense());
        assertEquals("ABC1D23", courier.getVehiclePlate());
        assertEquals("AVAILABLE", courier.getStatus());
    }

    @Test
    @DisplayName("Auditoria, CNH e placa começam vazias")
    void deveNascerSemAuditoria() {
        CourierJpaEntity courier = new CourierJpaEntity();

        assertNull(courier.getCreatedAt());
        assertNull(courier.getDriverLicense());
        assertNull(courier.getVehiclePlate());
    }
}
