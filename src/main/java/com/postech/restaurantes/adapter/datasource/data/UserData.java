package com.postech.restaurantes.adapter.datasource.data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Usuário como a origem de dados o conhece. Em inserções, id e auditoria vêm nulos e a origem
 * de dados devolve o registro preenchido. Cada perfil é nulo quando o usuário não o tem.
 */
public record UserData(UUID id, String name, String email, String login, String passwordHash,
                       OwnerData owner, ClientData client, CourierData courier, AdminData admin,
                       List<UserAddressData> addresses, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
}
