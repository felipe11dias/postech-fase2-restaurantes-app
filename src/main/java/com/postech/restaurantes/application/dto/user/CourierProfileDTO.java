package com.postech.restaurantes.application.dto.user;

import com.postech.restaurantes.domain.entity.courier.CourierProfile;
import com.postech.restaurantes.domain.entity.courier.CourierVehicleType;

/**
 * Perfil de entregador como chega aos casos de uso. O tipo de veículo vem como texto e passa por
 * {@code CourierVehicleType.from}, para um valor desconhecido produzir a mensagem do domínio. O status
 * não vem: todo entregador novo começa fora de serviço.
 */
public record CourierProfileDTO(String cpf, String phone, String vehicleType, String driverLicense,
                                String vehiclePlate) implements UserProfileDTO {

    public CourierProfile toEntity() {
        return CourierProfile.create(cpf, phone, CourierVehicleType.from(vehicleType), driverLicense, vehiclePlate);
    }
}
