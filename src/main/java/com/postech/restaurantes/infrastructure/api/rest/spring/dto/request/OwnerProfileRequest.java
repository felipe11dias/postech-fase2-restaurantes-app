package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.user.OwnerProfileDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Perfil de dono de restaurante no corpo HTTP. Validação sintática; os verificadores do CNPJ são do domínio. */
public record OwnerProfileRequest(
        @Schema(description = "Com ou sem máscara; numérico ou alfanumérico", example = "11.222.333/0001-81")
        @NotBlank @Size(max = 18) String cnpj,
        @Schema(example = "Sabor & Arte Restaurantes Ltda") @NotBlank @Size(max = 150) String legalName,
        @Schema(example = "(11) 3123-4567") @NotBlank @Size(max = 20) String businessPhone) {

    public OwnerProfileDTO toDTO() {
        return new OwnerProfileDTO(cnpj, legalName, businessPhone);
    }
}
