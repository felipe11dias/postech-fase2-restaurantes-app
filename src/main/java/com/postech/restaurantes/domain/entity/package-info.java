/**
 * Entidades de domínio, organizadas em subpacotes por agregado (user, address, ...). Construtor
 * privado e duas fábricas: create(...) para o novo, sem id, e restore(...) para o que já existe;
 * as duas passam pela mesma validação, e os setters revalidam (Aula 03; Clean Code, cap. 6).
 * Ver docs/arquitetura/01-entidades.md.
 */
package com.postech.restaurantes.domain.entity;
