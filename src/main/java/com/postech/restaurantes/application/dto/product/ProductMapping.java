package com.postech.restaurantes.application.dto.product;

import com.postech.restaurantes.domain.entity.product.ProductOptionGroup;
import com.postech.restaurantes.domain.entity.product.ProductOptionValue;
import com.postech.restaurantes.domain.Guard;
import java.util.Objects;
import java.util.List;

/** Conversão da entrada simples dos casos de uso para as partes do domínio. */
public final class ProductMapping {

    private ProductMapping() {
    }

    public static List<ProductOptionGroup> toGroups(List<ProductOptionGroupDTO> groups) {
        Guard.requireNonNull(groups, "Grupos de opções inválidos");
        Guard.require(groups.stream().noneMatch(Objects::isNull), "Grupo de opções inválido");
        return groups.stream().map(group -> new ProductOptionGroup(
                group.name(),
                group.required(),
                group.minSelections(),
                group.maxSelections(),
                Guard.requireNonNull(group.values(), "Valores do grupo de opções inválidos").stream()
                        .peek(value -> Guard.requireNonNull(value, "Valor de opção inválido"))
                        .map(value -> new ProductOptionValue(value.name(), value.additionalPrice()))
                        .toList())).toList();
    }
}
