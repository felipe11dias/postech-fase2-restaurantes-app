package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.user.AdminProfileDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Perfil de administrador no corpo HTTP. Só um administrador o inclui ou altera. */
public record AdminProfileRequest(
        @Schema(description = "Código de funcionário, único", example = "ADM-0042") @NotBlank @Size(max = 50)
        String employeeCode,
        @Schema(description = "Opcional", example = "Operações") @Size(max = 100) String department,
        @Schema(description = "Opcional; ausente vale false", example = "false") Boolean superAdmin) {

    public AdminProfileDTO toDTO() {
        return new AdminProfileDTO(employeeCode, department, superAdmin);
    }
}
