package com.postech.restaurantes.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.postech.restaurantes.domain.entity.address.Address;
import com.postech.restaurantes.domain.entity.user.Role;
import com.postech.restaurantes.domain.entity.user.RoleName;
import com.postech.restaurantes.domain.entity.user.User;
import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * O filtro apenas traduz o cabeçalho em contexto de segurança. Recusar a requisição é da
 * configuração de autorização — por isso, em todo caso abaixo, a cadeia segue adiante.
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-trinta-e-dois-bytes-para-hmac-sha256";
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 12, 0);
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private JwtTokenIssuer issuer;
    private JwtAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        issuer = new JwtTokenIssuer(new JwtProperties(SECRET, Duration.ofHours(1)),
                Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
        filter = new JwtAuthenticationFilter(issuer);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    private static User usuario() {
        return User.restore(USER_ID, "João Silva", "joao.silva@email.com", "joao.silva", "$2a$10$hash",
                Set.of(Role.restore(UUID.randomUUID(), RoleName.ROLE_ADMIN)), List.<Address>of(),
                AGORA.minusDays(1), AGORA);
    }

    @Test
    @DisplayName("Bearer válido popula o contexto com o portador e seus papéis")
    void deveAutenticarComTokenValido() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + issuer.issue(usuario()).token());

        filter.doFilter(request, response, chain);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(autenticacao.getPrincipal() instanceof AuthenticatedUser);
        assertEquals("joao.silva", autenticacao.getName());
        assertEquals(List.of("ROLE_ADMIN"),
                autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Sem cabeçalho, a requisição segue anônima")
    void deveSeguirSemCabecalho() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic dXNlcjpzZW5oYQ==", "bearer minusculo", "Token abc", ""})
    @DisplayName("Cabeçalho com outro esquema é ignorado")
    void deveIgnorarOutroEsquema(String header) throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, header);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer com token inválido não autentica, e a requisição segue anônima")
    void deveSeguirAnonimoComTokenInvalido() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-que-nao-vale");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
