package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Dados de apoio compartilhados pelos testes de caso de uso. */
public final class UseCaseFixtures {

    public static final UUID USER_ID = UUID.fromString("7295577e-afe6-4875-8bbf-d21c21860711");
    public static final UUID OTHER_ID = UUID.fromString("a0d64f5e-e511-4b52-871d-582d7a8b18d0");
    public static final String HASH = "$2a$10$hash";
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 16, 12, 0);
    public static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    public static final Role CUSTOMER = Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER);
    public static final Role OWNER = Role.restore(UUID.randomUUID(), RoleName.ROLE_OWNER);
    public static final AddressDTO ADDRESS_DTO =
            new AddressDTO("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000");

    private UseCaseFixtures() {
    }

    public static User existingUser() {
        return User.restore(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH,
                Set.of(CUSTOMER), List.of(ADDRESS_DTO.toEntity()), NOW.minusDays(1), NOW.minusDays(1));
    }

    public static User otherUser() {
        return User.restore(OTHER_ID, "Ana", "ana@email.com", "ana", HASH, Set.of(CUSTOMER), List.of(),
                NOW.minusDays(2), NOW.minusDays(2));
    }

    public static Address address() {
        return ADDRESS_DTO.toEntity();
    }

    public static Clock clockAt(LocalDateTime moment) {
        return Clock.fixed(moment.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    }

    public static Instant instant(LocalDateTime moment) {
        return moment.toInstant(ZoneOffset.UTC);
    }
}
