/**
 * Mapeamento do agregado de usuário: UserJpaEntity, o repositório Spring Data e a origem de dados
 * que traduz entidade JPA ↔ record de adapter/datasource/data. As partes do agregado ficam em
 * subpacotes, espelhando o domínio: {@code address}, {@code role} e {@code password}. Este pacote
 * depende deles e nenhum depende de volta — por isso o endereço não referencia o usuário (a
 * associação é unidirecional), senão os dois pacotes formariam um ciclo.
 */
package com.postech.restaurantes.infrastructure.persistence.jpa.user;
