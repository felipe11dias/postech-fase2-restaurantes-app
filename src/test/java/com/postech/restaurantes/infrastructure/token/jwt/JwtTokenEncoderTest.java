package com.postech.restaurantes.infrastructure.token.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.adapter.service.data.TokenClaimsData;
import com.postech.restaurantes.application.dto.auth.IssuedToken;
import com.postech.restaurantes.infrastructure.web.security.AuthenticatedUser;
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

class JwtTokenEncoderTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-trinta-e-dois-bytes-para-hmac-sha256";
    private static final Duration VALIDITY = Duration.ofHours(1);
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 12, 0);
    private static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final TokenClaimsData CLAIMS = new TokenClaimsData(USER_ID, "joao.silva", Set.of("ROLE_CUSTOMER"));

    private final JwtTokenEncoder encoder = new JwtTokenEncoder(new JwtProperties(SECRET, VALIDITY), RELOGIO);

    @Test
    @DisplayName("Token emitido carrega id, login e papéis do usuário autenticado")
    void deveEmitirComOsDadosDoUsuario() {
        IssuedToken emitido = encoder.encode(CLAIMS);

        AuthenticatedUser lido = encoder.read(emitido.token()).orElseThrow();
        assertEquals(USER_ID, lido.id());
        assertEquals("joao.silva", lido.login());
        assertEquals(Set.of("ROLE_CUSTOMER"), lido.roles());
    }

    @Test
    @DisplayName("A expiração é calculada a partir do relógio injetado, não do relógio do sistema")
    void deveCalcularAExpiracaoPeloRelogio() {
        assertEquals(AGORA.plus(VALIDITY), encoder.encode(CLAIMS).expiresAt());
    }

    @Test
    @DisplayName("O token carrega só o portador (sub, login, papéis) e as datas — nada além dos claims recebidos")
    void naoDeveCarregarNadaAlemDosClaims() {
        String token = encoder.encode(CLAIMS).token();

        var payload = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(RELOGIO.instant()))
                .build().parseSignedClaims(token).getPayload();
        assertEquals(Set.of("sub", "login", "roles", "iat", "exp"), payload.keySet());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "nao-e-um-jwt", "a.b.c"})
    @DisplayName("Token ausente ou malformado é recusado sem explicação")
    void deveRecusarTokenInvalido(String token) {
        assertTrue(encoder.read(token).isEmpty());
    }

    @Test
    @DisplayName("Token assinado com outro segredo é recusado")
    void deveRecusarAssinaturaDeOutroSegredo() {
        JwtTokenEncoder outro = new JwtTokenEncoder(
                new JwtProperties("outro-segredo-de-teste-com-mais-de-trinta-e-dois-bytes-aqui", VALIDITY), RELOGIO);

        assertTrue(encoder.read(outro.encode(CLAIMS).token()).isEmpty());
    }

    @Test
    @DisplayName("Token expirado é recusado")
    void deveRecusarTokenExpirado() {
        String token = encoder.encode(CLAIMS).token();
        Clock depois = Clock.fixed(AGORA.plus(VALIDITY).plusMinutes(1).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        JwtTokenEncoder noFuturo = new JwtTokenEncoder(new JwtProperties(SECRET, VALIDITY), depois);

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

        assertEquals(Set.of(), encoder.read(token).orElseThrow().roles());
    }

    @Test
    @DisplayName("Token com assinatura válida mas sem dono (sub) é recusado, e não quebra")
    void deveRecusarTokenSemDono() {
        String token = tokenAssinado(null, "joao.silva");

        assertTrue(encoder.read(token).isEmpty());
    }

    @Test
    @DisplayName("Token com assinatura válida mas sem login é recusado: a auditoria ficaria sem autor")
    void deveRecusarTokenSemLogin() {
        String token = tokenAssinado(USER_ID.toString(), null);

        assertTrue(encoder.read(token).isEmpty());
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
        JwtTokenEncoder comFuso = new JwtTokenEncoder(new JwtProperties(SECRET, VALIDITY),
                Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), saoPaulo));

        assertEquals(LocalDateTime.ofInstant(AGORA.toInstant(ZoneOffset.UTC).plus(VALIDITY), saoPaulo),
                comFuso.encode(CLAIMS).expiresAt());
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
