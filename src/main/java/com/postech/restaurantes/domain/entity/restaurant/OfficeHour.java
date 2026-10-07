package com.postech.restaurantes.domain.entity.restaurant;

import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.exception.InvariantViolationException;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Um intervalo de funcionamento do restaurante num dia da semana — uma linha de
 * {@code restaurant_office_hours}. É valor, parte do agregado {@link Restaurant}: dois horários com o
 * mesmo dia, abertura e fechamento são o mesmo horário.
 *
 * <p>Invariantes: dia, abertura e fechamento informados; abertura diferente do fechamento. Fechamento
 * antes da abertura quer dizer que o expediente vira a meia-noite (sexta das 18h às 2h termina no
 * sábado) — a mesma leitura do horário único que havia antes.
 *
 * <p>O dia é o {@link DayOfWeek} do JDK: tem os mesmos sete valores do tipo {@code day_of_week} do
 * modelo, e criar um enum igual seria duplicação.
 */
public record OfficeHour(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {

    private static final int SECONDS_PER_DAY = 24 * 60 * 60;
    private static final int SECONDS_PER_WEEK = 7 * SECONDS_PER_DAY;

    public OfficeHour {
        Guard.requireNonNull(dayOfWeek, "Dia da semana inválido");
        Guard.requireNonNull(startTime, "Horário de abertura inválido");
        Guard.requireNonNull(endTime, "Horário de fechamento inválido");
        Guard.require(!startTime.equals(endTime), "Horários de abertura e fechamento não podem ser iguais");
    }

    /** Converte o nome textual do dia sem diferenciar maiúsculas, com a mensagem do domínio. */
    public static DayOfWeek dayOf(String name) {
        String normalized = Guard.requireNonBlank(name, "Dia da semana inválido").toUpperCase();
        for (DayOfWeek candidate : DayOfWeek.values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new InvariantViolationException("Dia da semana inválido: " + name);
    }

    /** O expediente termina no dia seguinte. */
    public boolean crossesMidnight() {
        return endTime.isBefore(startTime);
    }

    /**
     * Se os dois intervalos se sobrepõem em algum momento da semana — inclusive quando um vira a
     * meia-noite e invade o dia seguinte (segunda das 22h às 2h colide com terça a partir da 1h), e quando
     * o de domingo invade a segunda.
     */
    public boolean overlaps(OfficeHour other) {
        Guard.requireNonNull(other, "Horário inválido");
        return other.contains(weekStart()) || contains(other.weekStart());
    }

    /** O instante da semana, em segundos a partir de segunda 0h, em que o intervalo começa. */
    private int weekStart() {
        return (dayOfWeek.getValue() - 1) * SECONDS_PER_DAY + startTime.toSecondOfDay();
    }

    private int lengthInSeconds() {
        int seconds = endTime.toSecondOfDay() - startTime.toSecondOfDay();
        return seconds > 0 ? seconds : seconds + SECONDS_PER_DAY;
    }

    /** Se o instante da semana cai dentro do intervalo, que começa fechado e termina aberto. */
    private boolean contains(int weekSecond) {
        int sinceStart = Math.floorMod(weekSecond - weekStart(), SECONDS_PER_WEEK);
        return sinceStart < lengthInSeconds();
    }
}
