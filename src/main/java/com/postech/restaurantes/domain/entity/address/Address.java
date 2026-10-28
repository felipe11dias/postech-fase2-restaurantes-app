package com.postech.restaurantes.domain.entity.address;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.location.city.City;
import com.postech.restaurantes.domain.vo.ZipCode;
import java.util.UUID;

/** Endereço de uma cidade existente. Os dados do catálogo são carregados na restauração. */
public final class Address {

    private final UUID id;
    private UUID cityId;
    private City city;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private ZipCode zipCode;

    private Address(UUID id) {
        this.id = id;
    }

    public static Address create(UUID cityId, String street, String number, String complement,
                                 String neighborhood, String zipCode) {
        return fill(new Address(null), cityId, street, number, complement, neighborhood, zipCode);
    }

    public static Address restore(UUID id, City city, String street, String number, String complement,
                                  String neighborhood, String zipCode) {
        Guard.requireNonNull(city, "Cidade do endereço inválida");
        Address address = fill(new Address(Guard.requireNonNull(id, "Id do endereço inválido")),
                city.getId(), street, number, complement, neighborhood, zipCode);
        address.city = city;
        return address;
    }

    private static Address fill(Address address, UUID cityId, String street, String number, String complement,
                                String neighborhood, String zipCode) {
        address.setCityId(cityId);
        address.setStreet(street);
        address.setNumber(number);
        address.setComplement(complement);
        address.setNeighborhood(neighborhood);
        address.setZipCode(ZipCode.of(zipCode));
        return address;
    }

    public void setCityId(UUID cityId) {
        UUID validated = Guard.requireNonNull(cityId, "Id da cidade do endereço inválido");
        if (!validated.equals(this.cityId)) {
            this.city = null;
        }
        this.cityId = validated;
    }

    public void setStreet(String street) {
        this.street = Guard.requireNonBlank(street, "Rua inválida");
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

    public void setZipCode(ZipCode zipCode) {
        this.zipCode = Guard.requireNonNull(zipCode, "CEP inválido");
    }

    public UUID getId() {
        return id;
    }

    public UUID getCityId() {
        return cityId;
    }

    public City getCity() {
        return city;
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

    public ZipCode getZipCode() {
        return zipCode;
    }

}
