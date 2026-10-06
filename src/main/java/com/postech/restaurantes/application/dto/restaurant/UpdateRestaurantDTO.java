package com.postech.restaurantes.application.dto.restaurant;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import java.util.List;
import java.util.UUID;

/** Dados de entrada para o caso de uso de atualização de restaurante. */
public record UpdateRestaurantDTO(
        UUID id,
        UUID userId,
        AddressDTO address,
        String name,
        List<OfficeHourDTO> officeHours
) {
}
