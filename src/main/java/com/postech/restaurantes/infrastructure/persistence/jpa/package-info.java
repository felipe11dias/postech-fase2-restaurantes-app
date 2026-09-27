/**
 * Módulo JPA (Hibernate + Spring Data). Implementa as origens de dados de adapter/datasource e
 * IUnitOfWork (transação via TransactionTemplate). Subpacotes: {@code audit} (colunas de
 * auditoria, comuns a toda entidade) e um por agregado, espelhando o domínio.
 *
 * <p>Não conhece segurança: o autor da auditoria chega por um Supplier ligado em {@code main}.
 * Substituir (ex.: JDBC puro): novo subpacote de {@code persistence} com as mesmas
 * implementações; o schema continua do Flyway.
 */
package com.postech.restaurantes.infrastructure.persistence.jpa;
