package com.postech.restaurantes.adapter.gateway.mapping;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.domain.entity.address.Address;

/** Endereço do domínio {@literal <->} registro da origem de dados. */
public final class AddressMapping {

    private AddressMapping() {
    }

    /** Reconstrói com {@code restore}: o que vem da origem de dados passa de novo pelas invariantes. */
    public static Address toEntity(AddressData data) {
        return Address.restore(data.id(), data.street(), data.number(), data.complement(),
                data.neighborhood(), data.city(), data.state(), data.zipCode());
    }

    public static AddressData toData(Address address) {
        return new AddressData(address.getId(), address.getStreet(), address.getNumber(), address.getComplement(),
                address.getNeighborhood(), address.getCity(), address.getState(), address.getZipCode().value());
    }
}
