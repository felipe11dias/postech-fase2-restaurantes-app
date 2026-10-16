/**
 * Tradução de partes que mais de um agregado tem, usada pelos gateways de adapter/gateway. Hoje,
 * o endereço: o usuário (por UserAddress) e o restaurante têm um {@code Address}, e a tradução
 * entidade {@literal <->} record é uma só — se o endereço mudar, muda para os dois. Não é gateway
 * (não implementa porta do núcleo), por isso fica fora de adapter.gateway.
 */
package com.postech.restaurantes.adapter.gateway.mapping;
