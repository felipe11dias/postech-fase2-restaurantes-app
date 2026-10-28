package com.postech.restaurantes.application.policy.cuisine;

import com.postech.restaurantes.application.gateway.ICuisineGateway;
import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import java.util.UUID;

/**
 * O nome identifica o tipo no catálogo: outro tipo com o mesmo nome (sem diferenciar maiúsculas) é conflito, com a
 * mensagem certa, e não a recusa genérica da restrição do banco. Usada pela criação e pela atualização, para a
 * regra existir uma vez só.
 */
public final class UniqueCuisineNamePolicy {

    private final ICuisineGateway cuisineGateway;

    private UniqueCuisineNamePolicy(ICuisineGateway cuisineGateway) {
        this.cuisineGateway = cuisineGateway;
    }

    public static UniqueCuisineNamePolicy create(ICuisineGateway cuisineGateway) {
        return new UniqueCuisineNamePolicy(cuisineGateway);
    }

    /**
     * @param name o nome já normalizado
     * @param self o id do próprio tipo, que não conta como "outro"; {@code null} num tipo novo
     */
    public void requireUnique(String name, UUID self) {
        if (cuisineGateway.findByName(name).filter(other -> !other.getId().equals(self)).isPresent()) {
            throw new DuplicateResourceException("Tipo de cozinha já cadastrado");
        }
    }
}
