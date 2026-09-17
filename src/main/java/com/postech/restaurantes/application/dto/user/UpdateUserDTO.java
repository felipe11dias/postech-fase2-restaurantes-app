package com.postech.restaurantes.application.dto.user;

import java.util.List;
import com.postech.restaurantes.application.dto.common.AddressDTO;

/** Entrada da atualização cadastral. Não inclui senha: a troca tem caso de uso próprio. */
public record UpdateUserDTO(String name, String email, String login, List<AddressDTO> addresses) {
}
