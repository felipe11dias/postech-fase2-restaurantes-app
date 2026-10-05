/**
 * Mapeamento do agregado de usuário: UserJpaEntity, o repositório Spring Data e a origem de dados
 * que traduz entidade JPA ↔ record de adapter/datasource/data. As partes do agregado ficam em
 * subpacotes: {@code address} (o vínculo {@code user_addresses}), {@code role} e {@code password};
 * o endereço em si é de {@code persistence/jpa/address}, compartilhado com o restaurante. Este
 * pacote depende deles e nenhum depende de volta — por isso as associações para as partes são
 * unidirecionais, senão os pacotes formariam ciclos.
 */
package com.postech.restaurantes.infrastructure.persistence.jpa.user;
