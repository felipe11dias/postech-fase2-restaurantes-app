package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.OfficeHourView;
import java.time.LocalTime;

/** Horário de funcionamento na resposta HTTP. */
public record OfficeHourResponse(String dayOfWeek, LocalTime startTime, LocalTime endTime) {

    public static OfficeHourResponse from(OfficeHourView view) {
        return new OfficeHourResponse(view.dayOfWeek(), view.startTime(), view.endTime());
    }
}
