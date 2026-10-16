/**
 * Regras de aplicação compartilhadas por mais de um caso de uso. Uma regra que dois casos de uso aplicam
 * igual mora aqui uma vez só, e não copiada em cada um (duplicação verdadeira, Martin). Como os casos
 * de uso, depende só do domínio e das portas de application/gateway; não é caso de uso — não tem run e
 * não representa um objetivo do ator. Ver docs/arquitetura/02-casos-de-uso.md.
 */
package com.postech.restaurantes.application.policy;
