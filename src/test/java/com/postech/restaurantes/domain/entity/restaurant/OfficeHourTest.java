package com.postech.restaurantes.domain.entity.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OfficeHourTest {

    private static OfficeHour horario(DayOfWeek dia, int abre, int fecha) {
        return new OfficeHour(dia, LocalTime.of(abre, 0), LocalTime.of(fecha, 0));
    }

    @Test
    @DisplayName("Recusa dia, abertura ou fechamento nulos e abertura igual ao fechamento")
    void deveRecusarHorarioInvalido() {
        LocalTime oito = LocalTime.of(8, 0);

        assertThrows(IllegalArgumentException.class, () -> new OfficeHour(null, oito, LocalTime.of(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> new OfficeHour(DayOfWeek.MONDAY, null, oito));
        assertThrows(IllegalArgumentException.class, () -> new OfficeHour(DayOfWeek.MONDAY, oito, null));
        IllegalArgumentException iguais =
                assertThrows(IllegalArgumentException.class, () -> new OfficeHour(DayOfWeek.MONDAY, oito, oito));
        assertEquals("Horários de abertura e fechamento não podem ser iguais", iguais.getMessage());
    }

    @Test
    @DisplayName("Fechamento antes da abertura é expediente que vira a meia-noite")
    void deveReconhecerAViradaDaMeiaNoite() {
        assertTrue(horario(DayOfWeek.FRIDAY, 18, 2).crossesMidnight());
        assertFalse(horario(DayOfWeek.FRIDAY, 8, 22).crossesMidnight());
    }

    @Test
    @DisplayName("Sobreposição no mesmo dia; intervalos que só se encostam não se sobrepõem")
    void deveDetectarSobreposicaoNoMesmoDia() {
        assertTrue(horario(DayOfWeek.MONDAY, 8, 14).overlaps(horario(DayOfWeek.MONDAY, 12, 18)));
        assertTrue(horario(DayOfWeek.MONDAY, 12, 18).overlaps(horario(DayOfWeek.MONDAY, 8, 14)));
        assertTrue(horario(DayOfWeek.MONDAY, 8, 22).overlaps(horario(DayOfWeek.MONDAY, 10, 12)));
        assertFalse(horario(DayOfWeek.MONDAY, 8, 12).overlaps(horario(DayOfWeek.MONDAY, 12, 18)));
        assertFalse(horario(DayOfWeek.MONDAY, 8, 12).overlaps(horario(DayOfWeek.TUESDAY, 8, 12)));
    }

    @Test
    @DisplayName("O que vira a meia-noite invade o dia seguinte, e o de domingo invade a segunda")
    void deveDetectarSobreposicaoAoVirarAMeiaNoite() {
        assertTrue(horario(DayOfWeek.MONDAY, 22, 2).overlaps(horario(DayOfWeek.TUESDAY, 1, 10)));
        assertFalse(horario(DayOfWeek.MONDAY, 22, 2).overlaps(horario(DayOfWeek.TUESDAY, 2, 10)));
        assertTrue(horario(DayOfWeek.SUNDAY, 20, 3).overlaps(horario(DayOfWeek.MONDAY, 0, 1)));
        assertFalse(horario(DayOfWeek.SUNDAY, 20, 3).overlaps(horario(DayOfWeek.SATURDAY, 20, 3)));
        assertThrows(IllegalArgumentException.class, () -> horario(DayOfWeek.MONDAY, 8, 9).overlaps(null));
    }

    @Test
    @DisplayName("Converte o nome do dia sem diferenciar maiúsculas; desconhecido com a mensagem do domínio")
    void deveConverterODia() {
        assertEquals(DayOfWeek.FRIDAY, OfficeHour.dayOf("friday"));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> OfficeHour.dayOf("feriado"));
        assertEquals("Dia da semana inválido: feriado", erro.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    @DisplayName("Recusa dia em branco")
    void deveRecusarDiaEmBranco(String dia) {
        assertThrows(IllegalArgumentException.class, () -> OfficeHour.dayOf(dia));
    }
}
