package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Quem é o dono de um restaurante. Porta declarada por quem a consome — a {@link RestaurantSecurity} —,
 * como a {@link ICurrentRolesReader}; a composição ({@code main}) a liga ao núcleo.
 *
 * <p>Contrato: restaurante inexistente devolve vazio, nunca exceção.
 */
public interface IRestaurantOwnerReader {

    Optional<UUID> ownerOf(UUID restaurantId);
}
