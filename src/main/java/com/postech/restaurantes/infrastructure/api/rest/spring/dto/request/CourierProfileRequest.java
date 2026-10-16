package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.user.CourierProfileDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Perfil de entregador no corpo HTTP. O tipo de veículo vem como texto e é convertido pelo domínio,
 * para um valor desconhecido produzir a mensagem do domínio, e não um erro de formato. O status não
 * vem: todo entregador começa fora de serviço.
 */
public record CourierProfileRequest(
        @Schema(description = "Com ou sem máscara; o mesmo do perfil de cliente, se houver", example = "111.444.777-35")
        @NotBlank @Size(max = 14) String cpf,
        @Schema(example = "(11) 91234-5678") @NotBlank @Size(max = 20) String phone,
        @Schema(allowableValues = {"ON_FOOT", "BICYCLE", "MOTORCYCLE", "CAR"}, example = "MOTORCYCLE")
        @NotBlank String vehicleType,
        @Schema(description = "CNH, 11 dígitos. Obrigatória para moto e carro; ausente a pé ou de bicicleta",
                example = "02650306461")
        @Size(max = 14) String driverLicense,
        @Schema(description = "ABC1234 ou ABC1D23. Obrigatória para moto e carro; ausente a pé ou de bicicleta",
                example = "ABC1D23")
        @Size(max = 8) String vehiclePlate) {

    public CourierProfileDTO toDTO() {
        return new CourierProfileDTO(cpf, phone, vehicleType, driverLicense, vehiclePlate);
    }
}
