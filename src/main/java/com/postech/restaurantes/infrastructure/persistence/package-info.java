/**
 * Frameworks & Drivers: persistência. Entidades JPA, repositórios Spring Data e as
 * implementações de I*DataSource. Nada aqui é conhecido pelas camadas de dentro — o núcleo
 * só enxerga as interfaces de adapter/datasource. Subpacotes por agregado, espelhando o
 * domínio; o que é transversal (auditoria, unidade de trabalho) fica neste pacote.
 */
package com.postech.restaurantes.infrastructure.persistence;
