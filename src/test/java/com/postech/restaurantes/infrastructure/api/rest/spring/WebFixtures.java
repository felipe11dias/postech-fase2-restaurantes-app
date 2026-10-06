package com.postech.restaurantes.infrastructure.api.rest.spring;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import com.postech.restaurantes.adapter.presenter.view.ClientProfileView;
import com.postech.restaurantes.adapter.presenter.view.UserAddressView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.AddressRequest;
import com.postech.restaurantes.infrastructure.api.rest.spring.dto.request.ClientProfileRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Dados de apoio dos testes da camada HTTP. */
public final class WebFixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);
    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID ADDRESS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    public static final UUID USER_ADDRESS_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    public static final AddressView ADDRESS_VIEW = new AddressView(ADDRESS_ID, "Rua das Flores", "100", "Apto 21",
            "Centro", "São Paulo", "SP", "01001000");
    public static final AddressRequest ADDRESS_REQUEST =
            new AddressRequest("Rua das Flores", "100", "Apto 21", "Centro", "São Paulo", "SP", "01001-000");

    public static final ClientProfileRequest CLIENT_REQUEST =
            new ClientProfileRequest("529.982.247-25", "(11) 91234-5678", null);

    public static final UserView USER_VIEW = new UserView(USER_ID, "João Silva", "joao.silva@email.com",
            "joao.silva", List.of("ROLE_CLIENT"), null, new ClientProfileView("52998224725", "11912345678", null),
            null, null, List.of(new UserAddressView(USER_ADDRESS_ID, "Casa", true, ADDRESS_VIEW)),
            NOW.minusDays(1), NOW);

    private WebFixtures() {
    }
}
