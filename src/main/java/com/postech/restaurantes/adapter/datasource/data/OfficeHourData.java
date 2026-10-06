package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalTime;

/** Horário de funcionamento como a origem de dados o conhece: o dia pelo nome (o tipo {@code day_of_week}). */
public record OfficeHourData(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
