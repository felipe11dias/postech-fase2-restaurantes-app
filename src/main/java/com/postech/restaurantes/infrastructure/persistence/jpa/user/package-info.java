/**
 * Mapeamento do agregado de usuário — usuário, papéis, endereços e tokens de redefinição:
 * entidades JPA, repositórios Spring Data e as origens de dados que traduzem entidade JPA ↔ record
 * de adapter/datasource/data. O endereço fica aqui porque é parte do agregado (a chave
 * estrangeira é do usuário); num pacote próprio, formava um ciclo com este.
 */
package com.postech.restaurantes.infrastructure.persistence.jpa.user;
