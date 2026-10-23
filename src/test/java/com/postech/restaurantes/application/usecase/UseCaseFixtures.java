package com.postech.restaurantes.application.usecase;

import com.postech.restaurantes.application.dto.common.AddressDTO;
import com.postech.restaurantes.application.dto.restaurant.OfficeHourDTO;
import com.postech.restaurantes.application.dto.user.UserAddressDTO;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.admin.AdminProfile;
import com.postech.restaurantes.domain.entity.client.ClientProfile;
import com.postech.restaurantes.domain.entity.owner.OwnerProfile;
import com.postech.restaurantes.domain.entity.restaurant.OfficeHour;
import com.postech.restaurantes.domain.entity.user.User;
import com.postech.restaurantes.domain.entity.user.UserAddress;
import com.postech.restaurantes.domain.entity.user.UserProfiles;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** Dados de apoio compartilhados pelos testes de caso de uso. */
public final class UseCaseFixtures {

    public static final UUID USER_ID = UUID.fromString("7295577e-afe6-4875-8bbf-d21c21860711");
    public static final UUID OTHER_ID = UUID.fromString("a0d64f5e-e511-4b52-871d-582d7a8b18d0");
    public static final UUID USER_ADDRESS_ID = UUID.fromString("5c6f1a2b-3d4e-4f50-8a61-7b8c9d0e1f23");
    public static final String HASH = "$2a$10$hash";
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 16, 12, 0);
    public static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"));
    public static final UserProfiles CLIENT =
            new UserProfiles(null, ClientProfile.restore("52998224725", "11912345678", null), null, null);
    public static final UserProfiles OWNER = new UserProfiles(
            OwnerProfile.restore("11222333000181", "Sabor Ltda", "1131234567"), null, null, null);
    public static final UserProfiles ADMIN =
            new UserProfiles(null, null, null, AdminProfile.restore("ADM-1", null, true));
    public static final AddressDTO ADDRESS_DTO =
            new AddressDTO("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000");
    public static final UserAddressDTO USER_ADDRESS_DTO = new UserAddressDTO(null, "Casa", true, ADDRESS_DTO);
    public static final List<OfficeHour> OFFICE_HOURS =
            List.of(new OfficeHour(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(22, 0)));
    public static final List<OfficeHourDTO> OFFICE_HOURS_DTO =
            List.of(new OfficeHourDTO("TUESDAY", LocalTime.of(9, 0), LocalTime.of(23, 0)));

    private UseCaseFixtures() {
    }

    public static User existingUser() {
        return User.restore(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", HASH,
                CLIENT, List.of(UserAddress.restore(USER_ADDRESS_ID, "Casa", true, ADDRESS_DTO.toEntity())),
                NOW.minusDays(1), NOW.minusDays(1));
    }

    public static User otherUser() {
        return User.restore(OTHER_ID, "Ana", "ana@email.com", "ana", HASH, CLIENT, List.of(),
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
