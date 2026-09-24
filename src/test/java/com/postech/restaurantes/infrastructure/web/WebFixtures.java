package com.postech.restaurantes.infrastructure.web;

import com.postech.restaurantes.adapter.presenter.view.AddressView;
import com.postech.restaurantes.adapter.presenter.view.RoleView;
import com.postech.restaurantes.adapter.presenter.view.UserView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Dados de apoio dos testes da camada HTTP. */
public final class WebFixtures {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);
    public static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID ROLE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID ADDRESS_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    public static final UserView USER_VIEW = new UserView(USER_ID, "João Silva", "joao.silva@email.com",
            "joao.silva", List.of(new RoleView(ROLE_ID, "ROLE_CUSTOMER")),
            List.of(new AddressView(ADDRESS_ID, "Rua das Flores", "100", "Apto 21", "Centro",
                    "São Paulo", "SP", "01001000")),
            NOW.minusDays(1), NOW);

    private WebFixtures() {
    }
}
