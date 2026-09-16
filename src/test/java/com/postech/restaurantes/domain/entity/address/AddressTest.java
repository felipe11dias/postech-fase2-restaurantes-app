package com.postech.restaurantes.domain.entity.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.vo.ZipCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AddressTest {

    private static Address valido() {
        return Address.create("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "sp", "01001-000");
    }

    @Test
    @DisplayName("Cria endereço válido normalizando UF e CEP")
    void deveCriarQuandoValido() {
        Address address = valido();

        assertNull(address.getId());
        assertEquals("Rua das Flores", address.getStreet());
        assertEquals("100", address.getNumber());
        assertEquals("Apto 21", address.getComplement());
        assertEquals("Centro", address.getNeighborhood());
        assertEquals("São Paulo", address.getCity());
        assertEquals("SP", address.getState());
        assertEquals(ZipCode.of("01001000"), address.getZipCode());
    }

    @Test
    @DisplayName("Restaura endereço com id conhecido")
    void deveRestaurarComId() {
        UUID id = UUID.randomUUID();

        Address address = Address.restore(id, "Rua A", null, null, null, "Cidade", "RJ", "20000000");

        assertEquals(id, address.getId());
        assertEquals("RJ", address.getState());
    }

    @Test
    @DisplayName("Recusa restauração sem id")
    void deveRecusarRestaurarQuandoIdNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> Address.restore(null, "Rua A", null, null, null, "Cidade", "RJ", "20000000"));
    }

    @Test
    @DisplayName("Campos opcionais em branco ficam ausentes")
    void deveNormalizarOpcionaisEmBranco() {
        Address address = Address.create("Rua A", "  ", "", null, "Cidade", "MG", "30000000");

        assertNull(address.getNumber());
        assertNull(address.getComplement());
        assertNull(address.getNeighborhood());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa rua em branco")
    void deveRecusarRuaEmBranco(String street) {
        assertThrows(IllegalArgumentException.class,
                () -> Address.create(street, null, null, null, "Cidade", "SP", "01001000"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Recusa cidade em branco")
    void deveRecusarCidadeEmBranco(String city) {
        assertThrows(IllegalArgumentException.class,
                () -> Address.create("Rua A", null, null, null, city, "SP", "01001000"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "S", "SPX", "S1", "12"})
    @DisplayName("Recusa UF que não tenha exatamente 2 letras")
    void deveRecusarUfInvalida(String state) {
        assertThrows(IllegalArgumentException.class,
                () -> Address.create("Rua A", null, null, null, "Cidade", state, "01001000"));
    }

    @Test
    @DisplayName("Recusa CEP inválido")
    void deveRecusarCepInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> Address.create("Rua A", null, null, null, "Cidade", "SP", "123"));
    }

    @Test
    @DisplayName("Recusa CEP nulo ao alterar")
    void deveRecusarCepNuloAoAlterar() {
        Address address = valido();

        assertThrows(IllegalArgumentException.class, () -> address.setZipCode(null));
    }

    @Test
    @DisplayName("Setters revalidam e a entidade não fica corrompida")
    void deveRevalidarNosSetters() {
        Address address = valido();

        assertThrows(IllegalArgumentException.class, () -> address.setStreet(""));
        assertThrows(IllegalArgumentException.class, () -> address.setCity(" "));
        assertThrows(IllegalArgumentException.class, () -> address.setState("ABC"));

        assertEquals("Rua das Flores", address.getStreet());
        assertEquals("São Paulo", address.getCity());
        assertEquals("SP", address.getState());
    }
}
