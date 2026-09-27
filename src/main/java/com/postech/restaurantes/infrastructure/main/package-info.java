/**
 * Main (Martin, Clean Architecture cap. 26): o componente mais sujo, e o único que conhece todos
 * os módulos. Monta os controllers de adaptação a partir das portas, fornece o relógio da
 * aplicação e liga um módulo ao outro quando um precisa do que o outro sabe — por exemplo, o
 * autor da auditoria (persistência) vem do usuário autenticado (segurança HTTP), sem que um
 * importe o outro. Guarda também as políticas da aplicação que não pertencem a tecnologia
 * alguma, como a validade do token de redefinição de senha.
 */
package com.postech.restaurantes.infrastructure.main;
