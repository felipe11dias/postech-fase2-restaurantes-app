package com.postech.restaurantes.infrastructure.persistence.jpa.address;

import com.postech.restaurantes.LocationFixtures;

import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_DATA;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.ADDRESS_ID;
import static com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.addressEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AddressJpaMappingTest {

    @Test
    @DisplayName("Registro vira linha nova, sem id: o id é emitido pelo Hibernate")
    void deveMontarLinhaNovaSemId() {
        AddressJpaEntity entity = AddressJpaMapping.toEntity(ADDRESS_DATA, com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.cityEntity());

        assertNull(entity.getId());
        assertEquals("Rua das Flores", entity.getStreet());
        assertEquals("Apto 21", entity.getComplement());
        assertEquals("01001000", entity.getZipCode());
    }

    @Test
    @DisplayName("Copiar para uma linha existente troca os campos e mantém o id dela")
    void deveCopiarMantendoOId() {
        AddressJpaEntity existente = addressEntity();
        AddressData novo = new AddressData(UUID.randomUUID(), LocationFixtures.cityId("Rio", "RJ"), "Av. B", null, null, null, "20000000", LocationFixtures.cityData("Rio", "RJ"));

        AddressJpaMapping.copy(novo, existente, com.postech.restaurantes.infrastructure.persistence.jpa.PersistenceFixtures.cityEntity());

        assertEquals(ADDRESS_ID, existente.getId());
        assertEquals("Av. B", existente.getStreet());
        assertNull(existente.getNumber());
        assertEquals(LocationFixtures.CITY_ID, existente.getCity().getId());
    }

    @Test
    @DisplayName("Linha vira registro com todos os campos")
    void deveTraduzirLinhaEmRegistro() {
        assertEquals(ADDRESS_DATA, AddressJpaMapping.toData(addressEntity()));
    }
}
