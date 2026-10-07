package com.postech.restaurantes.infrastructure.persistence.jpa.user.address;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.USER_ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.userAddressEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAddressJpaEntityTest {

    @Test
    @DisplayName("Guarda o vínculo — id, rótulo, padrão — e o endereço que ele possui")
    void deveGuardarOsCampos() {
        UserAddressJpaEntity userAddress = userAddressEntity();

        assertEquals(USER_ADDRESS_ID, userAddress.getId());
        assertEquals("Casa", userAddress.getLabel());
        assertTrue(userAddress.isDefaultAddress());
        assertEquals(ADDRESS_ID, userAddress.getAddress().getId());
    }

    @Test
    @DisplayName("Auditoria e autor começam vazios: quem preenche é o listener do Spring Data")
    void deveNascerSemAuditoria() {
        UserAddressJpaEntity userAddress = new UserAddressJpaEntity();

        assertNull(userAddress.getCreatedAt());
        assertNull(userAddress.getCreatedBy());
        assertFalse(userAddress.isDefaultAddress());
    }
}
