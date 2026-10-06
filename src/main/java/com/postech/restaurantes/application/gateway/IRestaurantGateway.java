package com.postech.restaurantes.application.gateway;

import com.postech.restaurantes.application.dto.common.PageRequest;
import com.postech.restaurantes.application.dto.common.PageResult;
import com.postech.restaurantes.domain.entity.restaurant.Restaurant;
import java.util.Optional;
import java.util.UUID;

/** Acesso a restaurantes, em termos de domínio. Quem implementa traduz para a origem de dados. */
public interface IRestaurantGateway {

    Optional<Restaurant> findById(UUID id);

    /** Busca por nome (sem diferenciar maiúsculas); nome nulo lista todos. */
    PageResult<Restaurant> search(String name, PageRequest request);

    /** Persiste um restaurante novo e devolve a instância com id e auditoria. */
    Restaurant insert(Restaurant restaurant);

    /** Persiste alterações de um restaurante existente e devolve a instância atualizada. */
    Restaurant update(Restaurant restaurant);

    /** Se o usuário é dono de algum restaurante — o perfil de dono não sai enquanto for. */
    boolean existsByUserId(UUID userId);

    void delete(UUID id);
}
