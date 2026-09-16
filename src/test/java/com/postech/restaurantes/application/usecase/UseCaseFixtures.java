package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.AddressDTO;
import com.postech.restaurantes.domain.entity.Address;
import com.postech.restaurantes.domain.entity.Role;
import com.postech.restaurantes.domain.entity.RoleName;
import com.postech.restaurantes.domain.entity.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Dados de apoio compartilhados pelos testes de caso de uso. */
final class UseCaseFixtures {

    static final UUID USER_ID = UUID.fromString("7295577e-afe6-4875-8bbf-d21c21860711");
    static final UUID OTHER_ID = UUID.fromString("a0d64f5e-e511-4b52-871d-582d7a8b18d0");
    static final String HASH = "$2a$10$hash";
    static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 16, 12, 0);
    static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    static final Role CUSTOMER = Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER);
    static final Role OWNER = Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER);
    static final AddressDTO ADDRESS_DTO =
            new AddressDTO("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000");

    private UseCaseFixtures() {
    }

    static User existingUser() {
        return User.restore(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH,
                Set.of(CUSTOMER), List.of(ADDRESS_DTO.toEntity()), NOW.minusDays(1), NOW.minusDays(1));
    }

    static User otherUser() {
        return User.restore(OTHER_ID, "Ana", "ana@email.com", "ana", HASH, Set.of(CUSTOMER), List.of(),
                NOW.minusDays(2), NOW.minusDays(2));
    }

    static Address address() {
        return ADDRESS_DTO.toEntity();
    }

    static Clock clockAt(LocalDateTime moment) {
        return Clock.fixed(moment.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }

    static Instant instant(LocalDateTime moment) {
        return moment.toInstant(ZoneOffset.UTC);
    }
}
