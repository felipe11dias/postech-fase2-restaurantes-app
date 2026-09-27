package com.postech.restaurantes.adapter.gateway;

import com.postech.restaurantes.adapter.service.ITokenEncoder;
import com.postech.restaurantes.adapter.service.data.TokenClaimsData;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.domain.Guard;
import com.postech.restaurantes.domain.entity.user.User;
import java.util.stream.Collectors;

/**
 * Tradutor entre o {@link User} do domínio e os dados que vão dentro do token. O formato do token
 * (JWT, opaco…) é do {@link ITokenEncoder}; o que identifica o portador é decidido aqui.
 */
public final class TokenGateway implements ITokenIssuer {

    private final ITokenEncoder encoder;

    private TokenGateway(ITokenEncoder encoder) {
        this.encoder = Guard.requireNonNull(encoder, "Codificador de token inválido");
    }

    public static TokenGateway create(ITokenEncoder encoder) {
        return new TokenGateway(encoder);
    }

    @Override
    public IssuedToken issue(User user) {
        return encoder.encode(new TokenClaimsData(user.getId(), user.getLogin(),
                user.getRoles().stream().map(role -> role.getName().name()).collect(Collectors.toSet())));
    }
}
