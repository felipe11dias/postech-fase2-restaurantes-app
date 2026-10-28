package com.postech.restaurantes.adapter.gateway.mapping;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.domain.entity.address.Address;

/** Endereço do domínio ↔ registro da origem de dados. */
public final class AddressMapping {
    private AddressMapping() {}

    public static Address toEntity(AddressData data) {
        return Address.restore(data.id(), LocationMapping.toEntity(data.city()), data.street(), data.number(),
                data.complement(), data.neighborhood(), data.zipCode());
    }

    public static AddressData toData(Address address) {
        return new AddressData(address.getId(), address.getCityId(), address.getStreet(), address.getNumber(),
                address.getComplement(), address.getNeighborhood(), address.getZipCode().value(),
                address.getCity() == null ? null : LocationMapping.toData(address.getCity()));
    }
}
