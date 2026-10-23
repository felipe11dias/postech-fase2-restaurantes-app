package com.postech.restaurantes.adapter;

import com.postech.restaurantes.adapter.datasource.data.AddressData;
import com.postech.restaurantes.adapter.datasource.data.AdminData;
import com.postech.restaurantes.adapter.datasource.data.ClientData;
import com.postech.restaurantes.adapter.datasource.data.CourierData;
import com.postech.restaurantes.adapter.datasource.data.OfficeHourData;
import com.postech.restaurantes.adapter.datasource.data.OwnerData;
import com.postech.restaurantes.adapter.datasource.data.PasswordResetTokenData;
import com.postech.restaurantes.adapter.datasource.data.UserAddressData;
import com.postech.restaurantes.adapter.datasource.data.UserData;
import com.postech.restaurantes.application.dto.restaurant.OfficeHourDTO;
import com.postech.restaurantes.application.gateway.IUnitOfWork;
import com.postech.restaurantes.domain.entity.restaurant.OfficeHour;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** Dados e dublês de apoio aos testes dos adaptadores. */
public final class AdapterFixtures {

    public static final UUID USER_ID = UUID.fromString("7295577e-afe6-4875-8bbf-d21c21860711");
    public static final UUID ADDRESS_ID = UUID.fromString("a0d64f5e-e511-4b52-871d-582d7a8b18d0");
    public static final UUID USER_ADDRESS_ID = UUID.fromString("5c6f1a2b-3d4e-4f50-8a61-7b8c9d0e1f23");
    public static final UUID TOKEN_ID = UUID.fromString("0b1f6d8e-6f4b-4b1e-9a3c-2f0e9d1c5a77");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 17, 12, 0);
    public static final String HASH = "$2a$10$hash";

    public static final OwnerData OWNER_DATA = new OwnerData("11222333000181", "Sabor Ltda", "1131234567");
    public static final ClientData CLIENT_DATA = new ClientData("52998224725", "11912345678", null);
    public static final CourierData COURIER_DATA =
            new CourierData("52998224725", "11912345678", "MOTORCYCLE", "02650306461", "ABC1D23", "AVAILABLE");
    public static final AdminData ADMIN_DATA = new AdminData("ADM-1", "Operações", true);
    public static final List<OfficeHourData> OFFICE_HOURS_DATA =
            List.of(new OfficeHourData("MONDAY", LocalTime.of(8, 0), LocalTime.of(22, 0)));
    public static final List<OfficeHour> OFFICE_HOURS =
            List.of(new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(22, 0)));
    public static final List<OfficeHourDTO> OFFICE_HOURS_DTO =
            List.of(new OfficeHourDTO("TUESDAY", LocalTime.of(9, 0), LocalTime.of(23, 0)));
    public static final AddressData ADDRESS_DATA =
            new AddressData(ADDRESS_ID, "Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001000");
    public static final UserAddressData USER_ADDRESS_DATA =
            new UserAddressData(USER_ADDRESS_ID, "Casa", true, ADDRESS_DATA);
    public static final UserData USER_DATA = new UserData(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva",
            HASH, null, CLIENT_DATA, null, null, List.of(USER_ADDRESS_DATA), NOW.minusDays(1), NOW);
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
