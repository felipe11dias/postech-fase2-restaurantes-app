package com.postech.restaurantes.adapter.datasource.data;

import java.util.UUID;

/** Endereço do usuário como a origem de dados o conhece: o vínculo, o rótulo, a marca de padrão e o endereço. */
public record UserAddressData(UUID id, String label, boolean isDefault, AddressData address) {
}
