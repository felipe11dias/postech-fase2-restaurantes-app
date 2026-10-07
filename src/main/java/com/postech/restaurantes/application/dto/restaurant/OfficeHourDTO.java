package com.postech.restaurantes.application.dto.restaurant;

import com.postech.restaurantes.domain.entity.restaurant.OfficeHour;
import java.time.LocalTime;
import java.util.List;

/**
 * Um horário de funcionamento como chega aos casos de uso. O dia vem como texto e passa por
 * {@code OfficeHour.dayOf}, para um valor desconhecido produzir a mensagem do domínio.
 */
public record OfficeHourDTO(String dayOfWeek, LocalTime startTime, LocalTime endTime) {

    public OfficeHour toEntity() {
        return new OfficeHour(OfficeHour.dayOf(dayOfWeek), startTime, endTime);
    }

    /** Lista ausente chega ao domínio como ausente, que a recusa com a mensagem dele. */
    public static List<OfficeHour> toEntities(List<OfficeHourDTO> officeHours) {
        return officeHours == null ? null
                : officeHours.stream().map(dto -> dto == null ? null : dto.toEntity()).toList();
    }
}
