/**
 * Módulo JWT (jjwt). Implementa ITokenIssuer (porta do núcleo, para o login) e IAccessTokenReader
 * (porta de web/security, para ler o Bearer): emitir e ler o mesmo formato mudam juntos (CCP), e
 * por isso ficam no mesmo módulo.
 *
 * <p>Substituir (ex.: token opaco guardado no banco): novo subpacote de {@code token} que
 * implemente as duas portas. A cadeia HTTP não muda, porque só conhece IAccessTokenReader.
 */
package com.postech.restaurantes.infrastructure.token.jwt;
