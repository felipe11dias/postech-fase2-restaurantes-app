package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.user.ClientProfileDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Perfil de cliente no corpo HTTP. Validação sintática; os verificadores do CPF são do domínio. */
public record ClientProfileRequest(
        @Schema(description = "Com ou sem máscara", example = "111.444.777-35") @NotBlank @Size(max = 14) String cpf,
        @Schema(example = "(11) 91234-5678") @NotBlank @Size(max = 20) String phone,
        @Schema(description = "Opcional; não pode ser futura", example = "1990-05-20") LocalDate birthDate) {

    public ClientProfileDTO toDTO() {
        return new ClientProfileDTO(cpf, phone, birthDate);
    }
}
