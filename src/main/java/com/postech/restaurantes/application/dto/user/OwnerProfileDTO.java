package com.postech.restaurantes.application.dto.user;

import com.postech.restaurantes.domain.entity.owner.OwnerProfile;

/** Perfil de dono de restaurante como chega aos casos de uso. A conversão para o domínio vive aqui. */
public record OwnerProfileDTO(String cnpj, String legalName, String businessPhone) {

    public OwnerProfile toEntity() {
        return OwnerProfile.create(cnpj, legalName, businessPhone);
    }
}
