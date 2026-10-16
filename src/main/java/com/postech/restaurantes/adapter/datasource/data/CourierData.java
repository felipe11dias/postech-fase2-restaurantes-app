package com.postech.restaurantes.adapter.datasource.data;

/**
 * Perfil de entregador como a origem de dados o conhece. Tipo de veículo e status vão pelo nome
 * (os valores dos tipos {@code courier_vehicle_type} e {@code courier_status} do banco).
 */
public record CourierData(String cpf, String phone, String vehicleType, String driverLicense, String vehiclePlate,
                          String status) {
}
