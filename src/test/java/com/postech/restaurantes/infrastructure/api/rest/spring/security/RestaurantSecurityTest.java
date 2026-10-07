package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class RestaurantSecurityTest {

    private static final UUID DONO = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESTAURANTE = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID DE_OUTRO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final RestaurantSecurity security = new RestaurantSecurity(id -> {
        if (RESTAURANTE.equals(id)) {
            return Optional.of(DONO);
        }
        return DE_OUTRO.equals(id) ? Optional.of(UUID.randomUUID()) : Optional.empty();
    });

    private static Authentication autenticado(Object principal) {
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    @DisplayName("O dono do restaurante é reconhecido")
    void deveReconhecerODono() {
        assertTrue(security.isOwner(RESTAURANTE, autenticado(new AuthenticatedUser(DONO, "dono", Set.of()))));
    }

    @Test
    @DisplayName("Restaurante de outro dono, inexistente, id nulo, sem autenticação ou outro principal: não é dono")
    void naoDeveReconhecerQuemNaoEDono() {
        Authentication dono = autenticado(new AuthenticatedUser(DONO, "dono", Set.of()));

        assertFalse(security.isOwner(DE_OUTRO, dono));
        assertFalse(security.isOwner(UUID.randomUUID(), dono));
        assertFalse(security.isOwner(null, dono));
        assertFalse(security.isOwner(RESTAURANTE, null));
        assertFalse(security.isOwner(RESTAURANTE, autenticado("anonimo")));
    }
}
