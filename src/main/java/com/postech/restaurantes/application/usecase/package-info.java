/**
 * Um caso de uso por intenção do ator, organizados em subpacotes por agregado/feature (user, auth, ...).
 * Construtor privado, create(portas...) e run(dto) como único método público (Aula 03). Orquestram
 * entidades e gateways; não conhecem HTTP, banco nem transação. Ver docs/arquitetura/02-casos-de-uso.md.
 */
package com.postech.restaurantes.application.usecase;
