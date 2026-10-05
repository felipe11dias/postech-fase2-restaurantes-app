package com.postech.restaurantes.infrastructure.persistence.jpa.user.role;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ROLE_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.roleEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleJpaEntityTest {

    @Test
    @DisplayName("Guarda e devolve id e nome")
    void deveGuardarOsCampos() {
        RoleJpaEntity role = roleEntity();

        assertEquals(ROLE_ID, role.getId());
        assertEquals("ROLE_CUSTOMER", role.getName());
    }
}
