package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.CourierProfileView;

/** Perfil de entregador na resposta HTTP; documentos sem máscara, placa em maiúsculas. */
public record CourierProfileResponse(String cpf, String phone, String vehicleType, String driverLicense,
                                     String vehiclePlate, String status) {

    public static CourierProfileResponse from(CourierProfileView view) {
        return view == null ? null : new CourierProfileResponse(view.cpf(), view.phone(), view.vehicleType(),
                view.driverLicense(), view.vehiclePlate(), view.status());
    }
}
