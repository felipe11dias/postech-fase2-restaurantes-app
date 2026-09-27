/**
 * Implementações das portas de application.gateway — o "tradutor" das Aulas 02 e 05 e o Adapter
 * de Freeman (cap. 7). Traduzem entidade {@literal <->} record de adapter/datasource (UserGateway, ...) ou
 * pedido do núcleo {@literal ->} chamada de adapter/service (PasswordResetMailGateway monta o e-mail,
 * TokenGateway monta os claims). Ver docs/arquitetura/03-adaptadores.md.
 */
package com.postech.restaurantes.adapter.gateway;
