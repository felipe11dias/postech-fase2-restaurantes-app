package com.postech.restaurantes.application.dto.user;

import com.postech.restaurantes.domain.entity.user.RoleName;
import java.util.List;
import java.util.Set;
import com.postech.restaurantes.application.dto.common.AddressDTO;

/** Entrada do autocadastro. A senha chega em claro e é transformada em hash pelo caso de uso. */
public record NewUserDTO(String name, String email, String login, String password,
                         Set<RoleName> roles, List<AddressDTO> addresses) {
}
