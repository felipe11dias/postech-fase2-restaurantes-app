package com.postech.restaurantes.infrastructure.persistence;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.stereotype.Component;

/**
 * Fonte do instante que a auditoria grava. Existe para que a aplicação tenha <strong>um único
 * relógio</strong>: o mesmo {@code Clock} injetado nos casos de uso é o que carimba
 * {@code created_at} e {@code last_updated_at}, em vez de uma segunda chamada a
 * {@code now()} escondida dentro do Spring Data.
 */
@Component("auditingDateTimeProvider")
public class ClockDateTimeProvider implements DateTimeProvider {

    private final Clock clock;

    public ClockDateTimeProvider(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(LocalDateTime.now(clock));
    }
}
