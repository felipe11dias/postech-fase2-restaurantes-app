package com.postech.restaurantes.adapter.datasource.data;

import java.util.UUID;

/** Endereço como a origem de dados o conhece. CEP sem máscara (8 dígitos). */
public record AddressData(UUID id, String street, String number, String complement, String neighborhood,
                          String city, String state, String zipCode) {
}
