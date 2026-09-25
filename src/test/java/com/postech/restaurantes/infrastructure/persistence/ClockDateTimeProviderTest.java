package com.postech.restaurantes.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClockDateTimeProviderTest {

    @Test
    @DisplayName("A auditoria carimba o instante do relógio da aplicação, não um now() próprio")
    void deveUsarORelogioDaAplicacao() {
        LocalDateTime instante = LocalDateTime.of(2026, 3, 10, 12, 0);
        ClockDateTimeProvider provider =
                new ClockDateTimeProvider(Clock.fixed(instante.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        assertEquals(instante, provider.getNow().orElseThrow());
    }

    @Test
    @DisplayName("O instante sai na precisão da coluna: nanossegundos são descartados, microssegundos mantidos")
    void deveCarimbarEmMicrossegundos() {
        LocalDateTime comNanos = LocalDateTime.of(2026, 3, 10, 12, 0, 0, 788_653_512);
        ClockDateTimeProvider provider =
                new ClockDateTimeProvider(Clock.fixed(comNanos.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));

        assertEquals(LocalDateTime.of(2026, 3, 10, 12, 0, 0, 788_653_000), provider.getNow().orElseThrow());
    }
}
