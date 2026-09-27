package com.postech.restaurantes.adapter.service;

import com.postech.restaurantes.adapter.service.data.TokenClaimsData;
import com.postech.restaurantes.application.dto.auth.IssuedToken;

/**
 * Codifica os dados do portador num token de acesso assinado, com a validade configurada. Não
 * conhece a entidade de domínio: recebe só o que vai dentro do token, já traduzido pelo gateway.
 */
public interface ITokenEncoder {

    IssuedToken encode(TokenClaimsData claims);
}
