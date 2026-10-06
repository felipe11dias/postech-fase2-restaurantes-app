package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalTime;

/** Horário de funcionamento na saída: o dia pelo nome ({@code MONDAY} a {@code SUNDAY}). */
public record OfficeHourView(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
