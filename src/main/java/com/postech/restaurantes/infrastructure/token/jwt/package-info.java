/**
 * Módulo JWT (jjwt). Implementa ITokenEncoder (adapter/service: codifica os claims que o
 * TokenGateway montou, para o login) e IAccessTokenReader (porta de api/rest/spring/security, para ler o
 * Bearer): emitir e ler o mesmo formato mudam juntos (CCP), e por isso ficam no mesmo módulo. Não
 * conhece o domínio — quem sabe que o token é de um User é o gateway do adaptador.
 *
 * <p>Substituir (ex.: token opaco guardado no banco): novo subpacote de {@code token} que
 * implemente as duas interfaces. A cadeia HTTP não muda, porque só conhece IAccessTokenReader.
 */
package com.postech.restaurantes.infrastructure.token.jwt;
