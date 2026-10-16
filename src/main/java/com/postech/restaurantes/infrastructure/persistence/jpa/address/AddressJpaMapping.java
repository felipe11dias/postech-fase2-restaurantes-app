package com.postech.restaurantes.infrastructure.persistence.jpa.address;

import com.postech.restaurantes.adapter.datasource.data.AddressData;

/**
 * Registro do adaptador {@literal <->} {@link AddressJpaEntity}, o mesmo para o usuário e para o
 * restaurante. A auditoria e o id nunca vêm do registro: o id é emitido pelo Hibernate.
 */
public final class AddressJpaMapping {

    private AddressJpaMapping() {
    }

    /** Linha nova, sem id. */
    public static AddressJpaEntity toEntity(AddressData data) {
        AddressJpaEntity entity = new AddressJpaEntity();
        copy(data, entity);
        return entity;
    }

    /** Copia os campos para uma linha existente, mantendo o id dela. */
    public static void copy(AddressData data, AddressJpaEntity entity) {
        entity.setStreet(data.street());
        entity.setNumber(data.number());
        entity.setComplement(data.complement());
        entity.setNeighborhood(data.neighborhood());
        entity.setCity(data.city());
        entity.setState(data.state());
        entity.setZipCode(data.zipCode());
    }

    public static AddressData toData(AddressJpaEntity entity) {
        return new AddressData(entity.getId(), entity.getStreet(), entity.getNumber(), entity.getComplement(),
                entity.getNeighborhood(), entity.getCity(), entity.getState(), entity.getZipCode());
    }
}
