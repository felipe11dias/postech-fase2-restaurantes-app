package com.postech.restaurantes.infrastructure.security;

import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.application.gateway.ITokenIssuer;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.User;
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
 * Implementação de {@link ITokenIssuer} com JWT assinado em HMAC-SHA256.
 *
 * <p>Emite e lê o <em>mesmo</em> formato de token, e é o único lugar do sistema que sabe que
 * o token de acesso é um JWT — para o núcleo ele é apenas um texto opaco com uma expiração.
 * O instante vem do {@link Clock} injetado, e não de {@code Instant.now()}, para que a
 * expiração seja verificável em teste.
 */
@Component
public class JwtTokenIssuer implements ITokenIssuer {

    private static final String BEARER_ROLES = "roles";
    private static final String BEARER_LOGIN = "login";

    private final SecretKey key;
    private final java.time.Duration expiration;
    private final Clock clock;

    public JwtTokenIssuer(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    @Override
    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expiration);
        String token = Jwts.builder()
                .subject(user.getId().toString())
                .claim(BEARER_LOGIN, user.getLogin())
                .claim(BEARER_ROLES, user.getRoles().stream().map(Role::getName).map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, LocalDateTime.ofInstant(expiresAt, clock.getZone()));
    }

    /**
     * Lê um token recebido. Devolve vazio para qualquer motivo de recusa — assinatura inválida,
     * expirado, malformado, ausente: quem só precisa saber "vale ou não vale" não deve receber
     * a explicação, que ajudaria apenas quem está sondando a API.
     */
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
