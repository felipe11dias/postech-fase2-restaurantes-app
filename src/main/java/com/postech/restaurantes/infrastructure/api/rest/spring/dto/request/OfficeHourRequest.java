package com.postech.restaurantes.infrastructure.api.rest.spring.dto.request;

import com.postech.restaurantes.application.dto.restaurant.OfficeHourDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.List;

/**
 * Um horário de funcionamento no corpo HTTP. O dia vem como texto e é convertido pelo domínio, para um
 * valor desconhecido produzir a mensagem do domínio, e não um erro de formato.
 */
public record OfficeHourRequest(
        @Schema(allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"},
                example = "MONDAY")
        @NotBlank String dayOfWeek,
        @Schema(example = "08:00:00") @NotNull LocalTime startTime,
        @Schema(description = "Antes da abertura, o expediente vira a meia-noite", example = "22:00:00")
        @NotNull LocalTime endTime) {

    public OfficeHourDTO toDTO() {
        return new OfficeHourDTO(dayOfWeek, startTime, endTime);
    }

    static List<OfficeHourDTO> toDTOs(List<OfficeHourRequest> officeHours) {
        return officeHours == null ? null : officeHours.stream().map(OfficeHourRequest::toDTO).toList();
    }
}
