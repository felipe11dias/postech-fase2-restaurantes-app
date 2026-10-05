/**
 * Agregado de usuário: User (raiz), UserProfiles (os perfis — dono, cliente, entregador,
 * administrador — e os papéis derivados deles) e UserAddress (endereço do usuário, com rótulo e a
 * marca de padrão). Os perfis ficam em {@code owner}, {@code client}, {@code courier} e
 * {@code admin}; o papel em {@code role}; o endereço em si em {@code address}; o token de
 * redefinição de senha em {@code password} — pacotes de que este depende, sem que nenhum deles
 * dependa de volta.
 */
package com.postech.restaurantes.domain.entity.user;
