package com.postech.restaurantes.application.dto.product;

import java.math.BigDecimal;

/** Dados de um valor de opção recebido pelo caso de uso. */
public record ProductOptionValueDTO(String name, BigDecimal additionalPrice) {
}
