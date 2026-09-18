package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Usuário como o cliente pode vê-lo: sem hash de senha. */
public record UserView(UUID id, String name, String email, String login, List<RoleView> roles,
                       List<AddressView> addresses, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
}
