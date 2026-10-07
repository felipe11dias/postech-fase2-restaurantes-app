/**
 * CASOS DE USO. Depende apenas de domain. Cada ação do sistema é uma classe com create(gateways...) e run(dto).
 *
 * <p>São as <em>Application Business Rules</em> (Martin, cap. 20) e o contrato de comportamento
 * diante de um ator com um objetivo (Cockburn): cenário principal e extensões, como no
 * CadastrarEstudanteUseCase da Aula 03. Regra de aplicação usada por mais de um caso de uso fica em
 * policy/<agregado>, uma vez só. Ver docs/arquitetura/02-casos-de-uso.md.
 */
package com.postech.restaurantes.application;
