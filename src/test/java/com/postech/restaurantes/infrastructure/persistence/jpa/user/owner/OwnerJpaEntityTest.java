package com.postech.restaurantes.infrastructure.persistence.jpa.user.owner;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ownerEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OwnerJpaEntityTest {

    @Test
    @DisplayName("Guarda o perfil de dono com o id do usuário")
    void deveGuardarOsCampos() {
        OwnerJpaEntity owner = ownerEntity();

        assertEquals(USER_ID, owner.getId());
        assertEquals("11222333000181", owner.getCnpj());
        assertEquals("Sabor Ltda", owner.getLegalName());
        assertEquals("1131234567", owner.getBusinessPhone());
    }

    @Test
    @DisplayName("Auditoria começa vazia: quem preenche é o listener do Spring Data")
    void deveNascerSemAuditoria() {
        OwnerJpaEntity owner = new OwnerJpaEntity();

        assertNull(owner.getCreatedAt());
        assertNull(owner.getCreatedBy());
    }
}
