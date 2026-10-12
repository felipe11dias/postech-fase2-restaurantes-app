package com.postech.restaurantes.domain.entity.address;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.vo.ZipCode;
import java.util.UUID;

/**
 * Endereço compartilhado pelos agregados. Rua e CEP são obrigatórios; a localização pode ser
 * informada por cidade/UF ou pelo id da cidade. A restauração exige a relação persistida.
 */
public final class Address {

    private static final int STATE_LENGTH = 2;

    private final UUID id;
    private UUID cityId;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private ZipCode zipCode;

    private Address(UUID id) {
        this.id = id;
    }

    public static Address create(String street, String number, String complement,
                                 String neighborhood, String city, String state, String zipCode) {
        return create(null, street, number, complement, neighborhood, city, state, zipCode);
    }

    public static Address create(UUID cityId, String street, String number, String complement,
                                 String neighborhood, String city, String state, String zipCode) {
        Address address = fill(new Address(null), street, number, complement, neighborhood, city, state, zipCode);
        if (cityId != null) {
            address.setCityId(cityId);
        }
        return address;
    }

    public static Address createForCity(UUID cityId, String street, String number, String complement,
                                        String neighborhood, String zipCode) {
        Address address = new Address(null);
        address.setCityId(cityId);
        address.setStreet(street);
        address.setNumber(number);
        address.setComplement(complement);
        address.setNeighborhood(neighborhood);
        address.setZipCode(ZipCode.of(zipCode));
        return address;
    }

    public static Address restore(UUID id, UUID cityId, String street, String number, String complement,
                                  String neighborhood, String city, String state, String zipCode) {
        Address address = new Address(Guard.requireNonNull(id, "Id do endereço inválido"));
        address.setCityId(cityId);
        return fill(address, street, number, complement, neighborhood, city, state, zipCode);
    }

    private static Address fill(Address address, String street, String number, String complement,
                                String neighborhood, String city, String state, String zipCode) {
        address.setStreet(street);
        address.setNumber(number);
        address.setComplement(complement);
        address.setNeighborhood(neighborhood);
        address.setCity(city);
        address.setState(state);
        address.setZipCode(ZipCode.of(zipCode));
        return address;
    }

    public void setStreet(String street) {
        this.street = Guard.requireNonBlank(street, "Rua inválida");
    }

    public void setCityId(UUID cityId) {
        this.cityId = Guard.requireNonNull(cityId, "Id da cidade do endereço inválido");
    }

    public void setNumber(String number) {
        this.number = Guard.trimToNull(number);
    }

    public void setComplement(String complement) {
        this.complement = Guard.trimToNull(complement);
    }

    public void setNeighborhood(String neighborhood) {
        this.neighborhood = Guard.trimToNull(neighborhood);
    }

    public void setCity(String city) {
        this.city = Guard.requireNonBlank(city, "Cidade inválida");
    }

    public void setState(String state) {
        String normalized = Guard.requireNonBlank(state, "UF inválida").toUpperCase();
        Guard.require(normalized.length() == STATE_LENGTH && normalized.chars().allMatch(Character::isLetter),
                "UF deve ter 2 letras");
        this.state = normalized;
    }

    public void setZipCode(ZipCode zipCode) {
        this.zipCode = Guard.requireNonNull(zipCode, "CEP inválido");
    }

    public UUID getId() {
        return id;
    }

    public UUID getCityId() {
        return cityId;
    }

    public String getStreet() {
        return street;
    }

    public String getNumber() {
        return number;
    }

    public String getComplement() {
        return complement;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public ZipCode getZipCode() {
        return zipCode;
    }
}
