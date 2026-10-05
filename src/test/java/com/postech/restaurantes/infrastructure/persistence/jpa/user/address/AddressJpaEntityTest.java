package com.postech.restaurantes.infrastructure.persistence.jpa.user.address;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.addressEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressJpaEntityTest {

    @Test
    @DisplayName("Guarda e devolve todos os campos, com o CEP sem máscara")
    void deveGuardarOsCampos() {
        AddressJpaEntity address = addressEntity();

        assertEquals(ADDRESS_ID, address.getId());
        assertEquals("Rua das Flores", address.getStreet());
        assertEquals("100", address.getNumber());
        assertEquals("Apto 21", address.getComplement());
        assertEquals("Centro", address.getNeighborhood());
        assertEquals("São Paulo", address.getCity());
        assertEquals("SP", address.getState());
        assertEquals("01001000", address.getZipCode());
    }
}
