package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.OwnerProfileView;

/** Perfil de dono na resposta HTTP; CNPJ e telefone sem máscara. */
public record OwnerProfileResponse(String cnpj, String legalName, String businessPhone) {

    public static OwnerProfileResponse from(OwnerProfileView view) {
        return view == null ? null : new OwnerProfileResponse(view.cnpj(), view.legalName(), view.businessPhone());
    }
}
