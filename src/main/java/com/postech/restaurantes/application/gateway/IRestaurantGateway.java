package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.util.Optional;
import java.util.UUID;

/** Acesso a restaurantes, em termos de domínio. Quem implementa traduz para a origem de dados. */
public interface IRestaurantGateway {

    Optional<Restaurant> findById(UUID id);

    /**
     * Busca por nome (sem diferenciar maiúsculas) e, se informado, só os restaurantes de um dono. Nome nulo
     * não filtra por nome; dono nulo não filtra por dono.
     */
    PageResult<Restaurant> search(String name, UUID ownerId, PageRequest request);

    /** Persiste um restaurante novo e devolve a instância com id e auditoria. */
    Restaurant insert(Restaurant restaurant);

    /** Persiste alterações de um restaurante existente e devolve a instância atualizada. */
    Restaurant update(Restaurant restaurant);

    /** Se o usuário é dono de algum restaurante — o perfil de dono não sai enquanto for. */
    boolean existsByUserId(UUID userId);

    void delete(UUID id);

    /** Remove todos os restaurantes do usuário (com o endereço de cada um), quando o usuário sai. */
    void deleteByUserId(UUID userId);
}
