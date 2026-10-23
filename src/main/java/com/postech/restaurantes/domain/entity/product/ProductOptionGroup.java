package com.postech.restaurantes.domain.entity.product;

import com.postech.restaurantes.domain.Guard;
import java.util.List;
import java.util.Objects;

/** Grupo de opções pertencente a um produto. */
public record ProductOptionGroup(
        String name,
        boolean required,
        int minSelections,
        int maxSelections,
        List<ProductOptionValue> values
) {

    public ProductOptionGroup {
        name = Guard.requireNonBlank(name, "Nome do grupo de opções inválido");
        Guard.require(minSelections >= 0, "Mínimo de seleções inválido");
        Guard.require(maxSelections >= 1 && minSelections <= maxSelections,
                "Limites de seleção do grupo de opções inválidos");
        Guard.require(!required || minSelections >= 1,
                "Grupo de opções obrigatório deve exigir ao menos uma seleção");
        Guard.requireNonNull(values, "Valores do grupo de opções inválidos");
        Guard.require(values.stream().noneMatch(Objects::isNull), "Valor de opção inválido");
        Guard.require(minSelections <= values.size(),
                "Mínimo de seleções não pode exceder os valores disponíveis");
        values = List.copyOf(values);
    }
}
