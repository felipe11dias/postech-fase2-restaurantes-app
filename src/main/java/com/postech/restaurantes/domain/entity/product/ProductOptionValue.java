package com.postech.restaurantes.domain.entity.product;

import com.postech.restaurantes.domain.Guard;
import java.math.BigDecimal;

/** Valor selecionável de um grupo de opções. */
public record ProductOptionValue(String name, BigDecimal additionalPrice) {

    public ProductOptionValue {
        name = Guard.requireNonBlank(name, "Nome do valor de opção inválido");
        Guard.requireNonNull(additionalPrice, "Preço adicional inválido");
        Guard.require(additionalPrice.signum() >= 0 && additionalPrice.scale() <= 2
                        && additionalPrice.precision() <= 10,
                "Preço adicional deve ser positivo e ter no máximo duas casas decimais");
    }
}
