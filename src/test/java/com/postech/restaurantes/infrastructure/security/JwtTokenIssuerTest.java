package com.postech.restaurantes.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtTokenIssuerTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-trinta-e-dois-bytes-para-hmac-sha256";
    private static final Duration VALIDITY = Duration.ofHours(1);
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 12, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(new JwtProperties(SECRET, VALIDITY), RELOGIO);

    private static User usuario() {
        return User.restore(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", "$2a$10$hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_CUSTOMER)), List.<Address>of(),
                AGORA.minusDays(1), AGORA);
    }

    @Test
    @DisplayName("Token emitido carrega id, login e papéis do usuário autenticado")
    void deveEmitirComOsDadosDoUsuario() {
        IssuedToken emitido = issuer.issue(usuario());

        AuthenticatedUser lido = issuer.read(emitido.token()).orElseThrow();
        assertEquals(USER_ID, lido.id());
        assertEquals("joao.silva", lido.login());
        assertEquals(Set.of("ROLE_CUSTOMER"), lido.roles());
    }

    @Test
    @DisplayName("A expiração é calculada a partir do relógio injetado, não do relógio do sistema")
    void deveCalcularAExpiracaoPeloRelogio() {
        assertEquals(AGORA.plus(VALIDITY), issuer.issue(usuario()).expiresAt());
    }

    @Test
    @DisplayName("O token não carrega a senha nem o hash")
    void naoDeveCarregarASenha() {
        String token = issuer.issue(usuario()).token();

        assertTrue(new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]), StandardCharsets.UTF_8).contains("joao.silva"));
        assertTrue(!token.contains("$2a$10$hash"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "nao-e-um-jwt", "a.b.c"})
    @DisplayName("Token ausente ou malformado é recusado sem explicação")
    void deveRecusarTokenInvalido(String token) {
        assertTrue(issuer.read(token).isEmpty());
    }

    @Test
    @DisplayName("Token assinado com outro segredo é recusado")
    void deveRecusarAssinaturaDeOutroSegredo() {
        JwtTokenIssuer outro = new JwtTokenIssuer(
                new JwtProperties("outro-segredo-de-teste-com-mais-de-trinta-e-dois-bytes-aqui", VALIDITY), RELOGIO);

        assertTrue(issuer.read(outro.issue(usuario()).token()).isEmpty());
    }

    @Test
    @DisplayName("Token expirado é recusado")
    void deveRecusarTokenExpirado() {
        String token = issuer.issue(usuario()).token();
        Clock depois = Clock.fixed(AGORA.plus(VALIDITY).plusMinutes(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        JwtTokenIssuer noFuturo = new JwtTokenIssuer(new JwtProperties(SECRET, VALIDITY), depois);

        assertTrue(noFuturo.read(token).isEmpty());
    }

    @Test
    @DisplayName("Token válido sem a reivindicação de papéis produz um portador sem papéis")
    void deveTratarTokenSemPapeis() {
        Instant agora = RELOGIO.instant();
        String token = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("login", "joao.silva")
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(VALIDITY)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertEquals(Set.of(), issuer.read(token).orElseThrow().roles());
    }

    @Test
    @DisplayName("Token com assinatura válida mas sem dono (sub) é recusado, e não quebra")
    void deveRecusarTokenSemDono() {
        String token = tokenAssinado(null, "joao.silva");

        assertTrue(issuer.read(token).isEmpty());
    }

    @Test
    @DisplayName("Token com assinatura válida mas sem login é recusado: a auditoria ficaria sem autor")
    void deveRecusarTokenSemLogin() {
        String token = tokenAssinado(USER_ID.toString(), null);

        assertTrue(issuer.read(token).isEmpty());
    }

    private static String tokenAssinado(String subject, String login) {
        Instant agora = RELOGIO.instant();
        var builder = Jwts.builder()
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(VALIDITY)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)));
        if (subject != null) {
            builder.subject(subject);
        }
        if (login != null) {
            builder.claim("login", login);
        }
        return builder.compact();
    }

    @Test
    @DisplayName("O relógio do emissor define o fuso da expiração devolvida")
    void deveUsarOFusoDoRelogio() {
        ZoneId saoPaulo = ZoneId.of("America/Sao_Paulo");
        JwtTokenIssuer comFuso = new JwtTokenIssuer(new JwtProperties(SECRET, VALIDITY),
                Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), saoPaulo));

        assertEquals(LocalDateTime.ofInstant(AGORA.toInstant(ZoneOffset.UTC).plus(VALIDITY), saoPaulo),
                comFuso.issue(usuario()).expiresAt());
    }

    @Test
    @DisplayName("Segredo curto demais para HMAC-SHA256 é recusado na configuração")
    void deveRecusarSegredoCurto() {
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties("curto", VALIDITY));
    }

    @Test
    @DisplayName("O segredo de exemplo publicado no repositório é recusado, mesmo tendo tamanho suficiente")
    void deveRecusarSegredoDeExemplo() {
        String exemplo = "troque-este-segredo-por-um-valor-grande-de-no-minimo-256-bits";

        assertTrue(exemplo.getBytes(StandardCharsets.UTF_8).length >= JwtProperties.MINIMUM_SECRET_BYTES,
                "o exemplo passaria na checagem de tamanho — é por isso que precisa de checagem própria");
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(exemplo, VALIDITY));
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties("  " + exemplo + "\n", VALIDITY));
    }

    @Test
    @DisplayName("Segredo ausente e expiração inválida são recusados na configuração")
    void deveRecusarConfiguracaoInvalida() {
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(null, VALIDITY));
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(SECRET, null));
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(SECRET, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new JwtProperties(SECRET, Duration.ofMinutes(-1)));
    }
}
