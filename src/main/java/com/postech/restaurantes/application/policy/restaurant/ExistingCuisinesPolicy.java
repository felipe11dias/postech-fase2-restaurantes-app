package com.postech.restaurantes.application.policy.restaurant;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.cuisine.Cuisine;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * O restaurante escolhe seus tipos de cozinha entre os do catálogo: um id que não existe nele é "não encontrado",
 * com a mensagem certa, e não a recusa da chave estrangeira do banco. Usada pelo cadastro e pela atualização do
 * restaurante, para a regra existir uma vez só.
 */
public final class ExistingCuisinesPolicy {

    private final ICuisineGateway cuisineGateway;

    private ExistingCuisinesPolicy(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static ExistingCuisinesPolicy create(ICuisineGateway cuisineGateway) {
        return new ExistingCuisinesPolicy(cuisineGateway);
    }

    /**
     * Os tipos de cozinha dos ids. O mesmo id repetido no pedido conta uma vez só: repetir não muda a escolha.
     *
     * @throws ResourceNotFoundException se algum id não está no catálogo
     */
    public List<Cuisine> requireAll(List<UUID> ids) {
        Guard.requireNonNull(ids, "Tipos de cozinha inválidos");
        Guard.require(ids.stream().noneMatch(Objects::isNull), "Id de tipo de cozinha inválido");
        List<UUID> distinct = ids.stream().distinct().toList();
        List<Cuisine> found = cuisineGateway.findAllById(distinct);
        if (found.size() != distinct.size()) {
            throw new ResourceNotFoundException("Tipo de cozinha não encontrado");
        }
        return found;
    }
}
