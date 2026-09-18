package com.postech.restaurantes.adapter;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.RoleData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Dados e dublês de apoio aos testes dos adaptadores. */
public final class AdapterFixtures {

    public static final UUID USER_ID = UUID.fromString("7295577e-afe6-4875-8bbf-d21c21860711");
    public static final UUID ROLE_ID = UUID.fromString("e19edd91-3b6e-4225-a721-ebfd6a5a576b");
    public static final UUID ADDRESS_ID = UUID.fromString("a0d64f5e-e511-4b52-871d-582d7a8b18d0");
    public static final UUID TOKEN_ID = UUID.fromString("0b1f6d8e-6f4b-4b1e-9a3c-2f0e9d1c5a77");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 12, 0);
    public static final String HASH = "$2a$10$hash";

    public static final RoleData CUSTOMER_DATA = new RoleData(ROLE_ID, "ROLE_CUSTOMER");
    public static final AddressData ADDRESS_DATA =
            new AddressData(ADDRESS_ID, "Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001000");
    public static final UserData USER_DATA = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva",
            HASH, Set.of(CUSTOMER_DATA), List.of(ADDRESS_DATA), NOW.minusDays(1), NOW);
    public static final PasswordResetTokenData TOKEN_DATA =
            new PasswordResetTokenData(TOKEN_ID, USER_ID, "hash-do-token", NOW.plusMinutes(30), false);

    private AdapterFixtures() {
    }

    /** Unidade de trabalho que apenas executa e conta quantas vezes foi usada. */
    public static final class CountingUnitOfWork implements IUnitOfWork {
        private final AtomicInteger executions = new AtomicInteger();

        @Override
        public <T> T execute(Supplier<T> work) {
            executions.incrementAndGet();
            return work.get();
        }

        public int executions() {
            return executions.get();
        }
    }
}
