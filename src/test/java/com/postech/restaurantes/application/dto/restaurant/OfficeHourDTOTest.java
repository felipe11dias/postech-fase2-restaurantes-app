package com.postech.restaurantes.application.dto.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.postech.restaurantes.domain.entity.restaurant.OfficeHour;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OfficeHourDTOTest {

    @Test
    @DisplayName("Converte o dia pelo domínio e mantém os horários")
    void deveConverterParaODominio() {
        OfficeHour horario = new OfficeHourDTO("saturday", LocalTime.of(11, 0), LocalTime.of(15, 0)).toEntity();

        assertEquals(new OfficeHour(DayOfWeek.SATURDAY, LocalTime.of(11, 0), LocalTime.of(15, 0)), horario);
    }

    @Test
    @DisplayName("Dia desconhecido é recusado com a mensagem do domínio")
    void deveRecusarDiaDesconhecido() {
        OfficeHourDTO dto = new OfficeHourDTO("feriado", LocalTime.of(11, 0), LocalTime.of(15, 0));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, dto::toEntity);

        assertEquals("Dia da semana inválido: feriado", erro.getMessage());
    }

    @Test
    @DisplayName("Lista ausente segue ausente até o domínio, e item nulo segue nulo")
    void deveRepassarAusencias() {
        assertNull(OfficeHourDTO.toEntities(null));
        assertEquals(Arrays.asList((OfficeHour) null), OfficeHourDTO.toEntities(Arrays.asList((OfficeHourDTO) null)));
        assertEquals(1, OfficeHourDTO.toEntities(List.of(new OfficeHourDTO("MONDAY", LocalTime.of(8, 0),
                LocalTime.of(9, 0)))).size());
    }
}
