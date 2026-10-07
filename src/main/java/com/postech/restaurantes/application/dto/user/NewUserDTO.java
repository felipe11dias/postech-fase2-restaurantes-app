package com.postech.restaurantes.application.dto.user;

import java.util.List;

/**
 * Entrada do autocadastro. A senha chega em claro e é transformada em hash pelo caso de uso. Os
 * perfis são opcionais um a um — ao menos um precisa vir — e não há perfil de administrador: ele não
 * se obtém por autocadastro.
 */
public record NewUserDTO(String name, String email, String login, String password,
                         OwnerProfileDTO owner, ClientProfileDTO client, CourierProfileDTO courier,
                         List<UserAddressDTO> addresses) {
}
