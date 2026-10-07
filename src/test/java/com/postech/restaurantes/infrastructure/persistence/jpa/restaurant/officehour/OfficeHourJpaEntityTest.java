package com.postech.restaurantes.infrastructure.persistence.jpa.restaurant.officehour;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OfficeHourJpaEntityTest {

    @Test
    @DisplayName("Guarda o horário com o dia pelo nome do valor do enum do banco")
    void deveGuardarOsCampos() {
        UUID id = UUID.randomUUID();
        OfficeHourJpaEntity horario = new OfficeHourJpaEntity();
        horario.setId(id);
        horario.setDayOfWeek("FRIDAY");
        horario.setStartTime(LocalTime.of(18, 0));
        horario.setEndTime(LocalTime.of(2, 0));

        assertEquals(id, horario.getId());
        assertEquals("FRIDAY", horario.getDayOfWeek());
        assertEquals(LocalTime.of(18, 0), horario.getStartTime());
        assertEquals(LocalTime.of(2, 0), horario.getEndTime());
    }

    @Test
    @DisplayName("Auditoria começa vazia: quem preenche é o listener do Spring Data")
    void deveNascerSemAuditoria() {
        assertNull(new OfficeHourJpaEntity().getCreatedAt());
    }
}
