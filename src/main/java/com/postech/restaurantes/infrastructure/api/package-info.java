/**
 * Módulo de entrega por API — "a Web é um detalhe" (Martin, cap. 31). O pacote diz o papel (api),
 * o subpacote o estilo (rest) e o seguinte a tecnologia (spring), como os demais módulos da
 * infraestrutura ({@code persistence/jpa}, {@code token/jwt}).
 *
 * <p>Outro canal (GraphQL, gRPC) seria um subpacote irmão chamando os mesmos controllers de
 * adaptação. Ver docs/arquitetura/04-frameworks-drivers.md.
 */
package com.postech.restaurantes.infrastructure.api;
