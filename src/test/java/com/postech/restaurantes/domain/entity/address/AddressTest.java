package com.postech.restaurantes.domain.entity.address;

import static org.junit.jupiter.api.Assertions.*;

import com.postech.restaurantes.LocationFixtures;
import com.postech.restaurantes.domain.exception.InvariantViolationException;
import com.postech.restaurantes.domain.vo.ZipCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AddressTest {
    private static Address validAddress() {
        return Address.create(LocationFixtures.CITY_ID, "Rua das Flores", "100", "Apto 21", "Centro", "01001-000");
    }

    @Test
    @DisplayName("Creates an address for an existing city and normalizes the zip code")
    void shouldCreateAddress() {
        Address address = validAddress();
        assertNull(address.getId());
        assertEquals(LocationFixtures.CITY_ID, address.getCityId());
        assertEquals("Rua das Flores", address.getStreet());
        assertEquals("100", address.getNumber());
        assertEquals("Apto 21", address.getComplement());
        assertEquals("Centro", address.getNeighborhood());
        assertEquals(ZipCode.of("01001000"), address.getZipCode());
    }

    @Test
    @DisplayName("Restores an address with the persisted city and state")
    void shouldRestoreAddress() {
        UUID id = UUID.randomUUID();
        var city = LocationFixtures.city("São Paulo", "SP");
        Address address = Address.restore(id, city, "Rua A", "1", null, "Centro", "01001000");
        assertEquals(id, address.getId());
        assertEquals(city.getId(), address.getCityId());
        assertSame(city, address.getCity());
        assertEquals("SP", address.getCity().getState().getAcronym());
    }

    @Test
    @DisplayName("Rejects creation or restoration without a city")
    void shouldRejectMissingCity() {
        assertThrows(InvariantViolationException.class,
                () -> Address.create(null, "Rua A", "1", null, "Centro", "01001000"));
        assertThrows(InvariantViolationException.class,
                () -> Address.restore(UUID.randomUUID(), null, "Rua A", "1", null, "Centro", "01001000"));
        var transientCity = com.postech.restaurantes.domain.entity.location.city.City.create(
                LocationFixtures.city("São Paulo", "SP").getState(), "Cidade");
        assertThrows(InvariantViolationException.class,
                () -> Address.restore(UUID.randomUUID(), transientCity, "Rua A", "1", null, "Centro", "01001000"));
    }

    @Test
    @DisplayName("Rejects restoration without an address id")
    void shouldRejectMissingId() {
        assertThrows(InvariantViolationException.class,
                () -> Address.restore(null, LocationFixtures.city("São Paulo", "SP"), "Rua A", "1", null,
                        "Centro", "01001000"));
    }

    @Test
    @DisplayName("Converts blank optional fields to null")
    void shouldNormalizeOptionalFields() {
        Address address = Address.create(LocationFixtures.CITY_ID, "Rua A", " ", "", null, "01001000");
        assertNull(address.getNumber());
        assertNull(address.getComplement());
        assertNull(address.getNeighborhood());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Rejects blank streets")
    void shouldRejectBlankStreet(String street) {
        assertThrows(InvariantViolationException.class,
                () -> Address.create(LocationFixtures.CITY_ID, street, "1", null, "Centro", "01001000"));
    }

    @Test
    @DisplayName("Rejects invalid zip codes and keeps valid values after invalid mutations")
    void shouldValidateMutations() {
        Address address = validAddress();
        assertThrows(InvariantViolationException.class, () -> address.setCityId(null));
        assertThrows(InvariantViolationException.class, () -> address.setStreet(" "));
        assertThrows(InvariantViolationException.class, () -> address.setZipCode(null));
        assertThrows(InvariantViolationException.class,
                () -> Address.create(LocationFixtures.CITY_ID, "Rua A", "1", null, "Centro", "123"));
        assertEquals(LocationFixtures.CITY_ID, address.getCityId());
        assertEquals("Rua das Flores", address.getStreet());
    }

    @Test
    @DisplayName("Keeps loaded city data for the same id and clears it when the reference changes")
    void shouldRefreshCityReference() {
        var city = LocationFixtures.city("São Paulo", "SP");
        Address address = Address.restore(UUID.randomUUID(), city, "Rua A", "1", null, "Centro", "01001000");
        address.setCityId(city.getId());
        assertSame(city, address.getCity());
        address.setCityId(LocationFixtures.RIO_CITY_ID);
        assertEquals(LocationFixtures.RIO_CITY_ID, address.getCityId());
        assertNull(address.getCity());
    }
}
