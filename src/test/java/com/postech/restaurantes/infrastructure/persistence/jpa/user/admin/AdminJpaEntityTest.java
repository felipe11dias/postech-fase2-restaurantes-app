package com.postech.restaurantes.infrastructure.persistence.jpa.user.admin;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.adminEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminJpaEntityTest {

    @Test
    @DisplayName("Guarda o perfil de administrador com o id do usuário")
    void deveGuardarOsCampos() {
        AdminJpaEntity admin = adminEntity();

        assertEquals(USER_ID, admin.getId());
        assertEquals("ADM-1", admin.getEmployeeCode());
        assertEquals("Operações", admin.getDepartment());
        assertTrue(admin.isSuperAdmin());
    }

    @Test
    @DisplayName("Auditoria começa vazia e o administrador novo não é super administrador")
    void deveNascerSemAuditoria() {
        AdminJpaEntity admin = new AdminJpaEntity();

        assertNull(admin.getCreatedAt());
        assertFalse(admin.isSuperAdmin());
    }
}
