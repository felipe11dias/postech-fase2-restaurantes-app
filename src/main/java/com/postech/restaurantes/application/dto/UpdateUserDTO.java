package com.postech.restaurantes.application.dto;

import java.util.List;

/** Entrada da atualização cadastral. Não inclui senha: a troca tem caso de uso próprio. */
public record UpdateUserDTO(String name, String email, String login, List<AddressDTO> addresses) {
}
