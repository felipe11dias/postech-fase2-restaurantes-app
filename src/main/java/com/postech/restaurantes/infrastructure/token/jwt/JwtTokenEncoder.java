package com.postech.restaurantes.infrastructure.token.jwt;

import com.postech.restaurantes.adapter.service.ITokenEncoder;
import com.postech.restaurantes.adapter.service.data.TokenClaimsData;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.AuthenticatedUser;
import com.postech.restaurantes.infrastructure.api.rest.spring.security.IAccessTokenReader;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Implementação de {@link ITokenEncoder} (emitir, para o login) e de {@link IAccessTokenReader}
 * (ler, para a cadeia HTTP) com JWT assinado em HMAC-SHA256. O algoritmo é fixado na emissão:
 * deixado ao jjwt, ele seria escolhido pelo tamanho do segredo (HS384 ou HS512 com o segredo de 48
 * bytes que o README sugere) e mudaria sozinho a cada troca de segredo. Não conhece o domínio:
 * recebe os dados do portador já traduzidos pelo gateway do adaptador.
 *
 * <p>Emite e lê o <em>mesmo</em> formato de token, e é o único lugar do sistema que sabe que
 * o token de acesso é um JWT — para o núcleo ele é apenas um texto opaco com uma expiração.
 * O instante vem do {@link Clock} injetado, e não de {@code Instant.now()}, para que a
 * expiração seja verificável em teste.
 */
@Component
public class JwtTokenEncoder implements ITokenEncoder, IAccessTokenReader {

    private static final String BEARER_ROLES = "roles";
    private static final String BEARER_LOGIN = "login";

    private final SecretKey key;
    private final java.time.Duration expiration;
    private final Clock clock;

    public JwtTokenEncoder(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    @Override
    public IssuedToken encode(TokenClaimsData claims) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(claims.subject().toString())
                .claim(BEARER_LOGIN, claims.login())
                .claim(BEARER_ROLES, claims.roles().stream().sorted().toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, LocalDateTime.ofInstant(expiresAt, clock.getZone()));
    }

    /**
     * Lê um token recebido. Devolve vazio para qualquer motivo de recusa — assinatura inválida,
     * expirado, malformado, ausente: quem só precisa saber "vale ou não vale" não deve receber
     * a explicação, que ajudaria apenas quem está sondando a API.
     */
    @Override
    public Optional<AuthenticatedUser> read(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser().verifyWith(key).clock(() -> Date.from(clock.instant())).build()
                    .parseSignedClaims(token).getPayload();
            // Assinatura válida não garante reivindicações presentes: sem "sub" não há dono para
            // a regra de posse, e sem "login" a auditoria não tem autor. Os dois são obrigatórios.
            String subject = claims.getSubject();
            String login = claims.get(BEARER_LOGIN, String.class);
            if (subject == null || login == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthenticatedUser(UUID.fromString(subject), login, papeis(claims)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Set<String> papeis(Claims claims) {
        List<?> valores = claims.get(BEARER_ROLES, List.class);
        if (valores == null) {
            return Set.of();
        }
        return valores.stream().map(String::valueOf).collect(Collectors
                .toCollection(LinkedHashSet::new));
    }
}
