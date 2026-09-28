package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import java.util.UUID;

/** Papel no corpo da resposta HTTP. */
public record RoleResponse(UUID id, String name) {
}
