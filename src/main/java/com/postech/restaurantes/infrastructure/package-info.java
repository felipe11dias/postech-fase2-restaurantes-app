/**
 * FRAMEWORKS & DRIVERS. Único pacote que conhece Spring, JPA, Hibernate e demais bibliotecas.
 *
 * <p>Cada subpacote é um <em>módulo-plugin</em> (Martin, Clean Architecture caps. 17 e 30-32): o
 * papel que ele cumpre dá o nome do pacote, e a tecnologia que o implementa, o do subpacote
 * ({@code persistence/jpa}, {@code token/jwt}, {@code mail/smtp}). Módulo nenhum conhece outro
 * módulo-irmão; só {@code main} liga as pontas. Trocar uma tecnologia é apagar um subpacote e
 * criar outro ao lado — e o InfrastructureModulesTest prova no build que nada mais dependia dele.
 *
 * <p>Os módulos implementam as interfaces de adapter/datasource e adapter/service; do núcleo, só as
 * portas técnicas (IPasswordEncoder, ISecureTokenGenerator, IUnitOfWork), em que não há tradução a
 * fazer. Ver docs/arquitetura/04-frameworks-drivers.md.
 */
package com.postech.restaurantes.infrastructure;
