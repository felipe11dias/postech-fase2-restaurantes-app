package com.postech.restaurantes.infrastructure.api.rest.spring.dto.response;

import com.postech.restaurantes.adapter.presenter.view.UserView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Usuário no corpo da resposta HTTP. Nasce de uma {@link UserView} — que já não tem campo de
 * senha —, de modo que não existe caminho de código capaz de serializar o hash. Os papéis saem pelo
 * nome, derivados dos perfis; cada perfil é nulo quando o usuário não o tem.
 */
public record UserResponse(UUID id, String name, String email, String login, List<String> roles,
                           OwnerProfileResponse owner, ClientProfileResponse client, CourierProfileResponse courier,
                           AdminProfileResponse admin, List<UserAddressResponse> addresses,
                           LocalDateTime createdAt, LocalDateTime lastUpdatedAt) {

    public static UserResponse from(UserView view) {
        return new UserResponse(view.id(), view.name(), view.email(), view.login(), view.roles(),
                OwnerProfileResponse.from(view.owner()), ClientProfileResponse.from(view.client()),
                CourierProfileResponse.from(view.courier()), AdminProfileResponse.from(view.admin()),
                view.addresses().stream().map(UserAddressResponse::from).toList(),
                view.createdAt(), view.lastUpdatedAt());
    }
}
