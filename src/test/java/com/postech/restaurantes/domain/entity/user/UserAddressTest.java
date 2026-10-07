package com.postech.restaurantes.domain.entity.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.domain.entity.address.Address;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserAddressTest {

    private static final Address ADDRESS =
            Address.create("Rua das Flores", "100", null, "Centro", "São Paulo", "SP", "01001000");

    @Test
    @DisplayName("Cria endereço do usuário sem id, com rótulo aparado e a marca de padrão")
    void deveCriarQuandoValido() {
        UserAddress userAddress = UserAddress.create("  Casa ", true, ADDRESS);

        assertNull(userAddress.getId());
        assertEquals("Casa", userAddress.getLabel());
        assertTrue(userAddress.isDefault());
        assertSame(ADDRESS, userAddress.getAddress());
    }

    @Test
    @DisplayName("Restaura endereço do usuário com id conhecido")
    void deveRestaurarComId() {
        UUID id = UUID.randomUUID();

        UserAddress userAddress = UserAddress.restore(id, "Trabalho", false, ADDRESS);

        assertEquals(id, userAddress.getId());
        assertFalse(userAddress.isDefault());
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> UserAddress.restore(null, "Casa", true, ADDRESS));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Rótulo em branco vira ausente: é opcional")
    void deveAceitarRotuloEmBranco(String label) {
        assertNull(UserAddress.create(label, true, ADDRESS).getLabel());
    }

    @Test
    @DisplayName("Recusa endereço nulo, na criação e no setter")
    void deveRecusarEnderecoNulo() {
        UserAddress userAddress = UserAddress.create("Casa", true, ADDRESS);

        assertThrows(IllegalArgumentException.class, () -> UserAddress.create("Casa", true, null));
        assertThrows(IllegalArgumentException.class, () -> userAddress.setAddress(null));
        assertSame(ADDRESS, userAddress.getAddress());
    }
}
