package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.AdminProfileView;

/** Perfil de administrador na resposta HTTP. */
public record AdminProfileResponse(String employeeCode, String department, boolean superAdmin) {

    public static AdminProfileResponse from(AdminProfileView view) {
        return view == null ? null
                : new AdminProfileResponse(view.employeeCode(), view.department(), view.superAdmin());
    }
}
