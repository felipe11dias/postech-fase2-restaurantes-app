package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Usuário como a origem de dados o conhece. Em inserções, id e auditoria vêm nulos e a origem
 * de dados devolve o registro preenchido.
 */
public record UserData(UUID id, String name, String email, String login, String passwordHash,
                       Set<RoleData> roles, List<AddressData> addresses,
                       LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
}
