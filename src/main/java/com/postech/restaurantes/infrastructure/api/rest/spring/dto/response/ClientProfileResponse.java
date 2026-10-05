package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.ClientProfileView;
import java.time.LocalDate;

/** Perfil de cliente na resposta HTTP; CPF e telefone sem máscara. */
public record ClientProfileResponse(String cpf, String phone, LocalDate birthDate) {

    public static ClientProfileResponse from(ClientProfileView view) {
        return view == null ? null : new ClientProfileResponse(view.cpf(), view.phone(), view.birthDate());
    }
}
