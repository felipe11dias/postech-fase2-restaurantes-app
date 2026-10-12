package com.postech.restaurantes.application.dto.common;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Dados de endereço recebidos pelos casos de uso. A conversão para o domínio vive aqui. */
public record AddressDTO(UUID cityId, String street, String number, String complement, String neighborhood,
                         String city, String state, String zipCode) {

    public AddressDTO(String street, String number, String complement, String neighborhood,
                      String city, String state, String zipCode) {
        this(null, street, number, complement, neighborhood, city, state, zipCode);
    }

    public Address toEntity() {
        if (cityId != null) {
            return Address.createForCity(cityId, street, number, complement, neighborhood, zipCode);
        }
        return Address.create(cityId, street, number, complement, neighborhood, city, state, zipCode);
    }

    /** Lista ausente é tratada como nenhum endereço; elemento nulo é entrada inválida. */
    public static List<Address> toEntities(List<AddressDTO> dtos) {
        if (dtos == null) {
            return List.of();
        }
        Guard.require(dtos.stream().noneMatch(Objects::isNull), "Endereço inválido");
        return dtos.stream().map(AddressDTO::toEntity).toList();
    }
}
