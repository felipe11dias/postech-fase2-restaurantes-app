package com.postech.restaurantes.adapter.presenter.view;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Usuário como o cliente pode vê-lo: sem hash de senha. Os papéis saem pelo nome, derivados dos
 * perfis; cada perfil é nulo quando o usuário não o tem.
 */
public record UserView(UUID id, String name, String email, String login, List<String> roles,
                       OwnerProfileView owner, ClientProfileView client, CourierProfileView courier,
                       AdminProfileView admin, List<UserAddressView> addresses,
                       LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {
}
