package com.postech.restaurantes.application.dto.user;

/**
 * Um perfil do usuário como chega a um caso de uso que inclui ou altera perfil. Selada: os tipos são
 * os do modelo, e o caso de uso trata cada um — com a garantia do compilador de que nenhum ficou de fora.
 */
public sealed interface UserProfileDTO permits OwnerProfileDTO, ClientProfileDTO, CourierProfileDTO, AdminProfileDTO {
}
