/**
 * Tabela {@code couriers}: perfil de entregador. {@code vehicle_type} e {@code status} são tipos
 * ENUM do banco, mapeados como texto. A chave primária é a do usuário, atribuída pela origem de
 * dados; o perfil não referencia o usuário — a associação é unidirecional, do lado de UserJpaEntity.
 */
package com.postech.restaurantes.infrastructure.persistence.jpa.user.courier;
