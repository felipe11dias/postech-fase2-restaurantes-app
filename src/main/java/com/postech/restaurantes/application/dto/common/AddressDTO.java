package com.postech.restaurantes.application.dto.common;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.address.Address;
import java.util.List;
import java.util.Objects;

/** Dados de endereço recebidos pelos casos de uso. A conversão para o domínio vive aqui. */
public record AddressDTO(String street, String number, String complement, String neighborhood,
                         String city, String state, String zipCode) {

    public Address toEntity() {
        return Address.create(street, number, complement, neighborhood, city, state, zipCode);
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
