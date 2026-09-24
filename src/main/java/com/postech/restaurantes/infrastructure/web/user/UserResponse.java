package com.postech.restaurantes.infrastructure.web.user;

import com.postech.restaurantes.adapter.presenter.view.UserView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Usuário no corpo da resposta HTTP. Nasce de uma {@link UserView} — que já não tem campo de
 * senha —, de modo que não existe caminho de código capaz de serializar o hash.
 */
public record UserResponse(UUID id, String name, String email, String login, List<RoleResponse> roles,
                           List<AddressResponse> addresses, LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {

    public static UserResponse from(UserView view) {
        return new UserResponse(view.id(), view.name(), view.email(), view.login(),
                view.roles().stream().map(role -> new RoleResponse(role.id(), role.name())).toList(),
                view.addresses().stream().map(AddressResponse::from).toList(),
                view.createdAt(), view.lastUpdatedAt());
    }
}
