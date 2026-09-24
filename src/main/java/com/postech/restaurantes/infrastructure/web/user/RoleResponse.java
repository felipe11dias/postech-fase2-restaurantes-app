package com.postech.restaurantes.infrastructure.web.user;

import java.util.UUID;

/** Papel no corpo da resposta HTTP. */
public record RoleResponse(UUID id, String name) {
}
