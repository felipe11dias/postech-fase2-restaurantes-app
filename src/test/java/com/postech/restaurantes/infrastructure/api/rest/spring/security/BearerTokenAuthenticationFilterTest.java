package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.FilterChain;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
 * O filtro contra um leitor de token falso: o teste não sabe — nem precisa saber — qual é o
 * formato do token. É a prova de que a cadeia HTTP depende só das portas {@link IAccessTokenReader} e
 * {@link ICurrentRolesReader}.
 */
class BearerTokenAuthenticationFilterTest {

    private static final String TOKEN_VALIDO = "token-valido";
    private static final AuthenticatedUser PORTADOR = new AuthenticatedUser(
            UUID.fromString("11111111-1111-1111-1111-111111111111"), "joao.silva", Set.of("ROLE_ADMIN"));

    private final List<String> lidos = new ArrayList<>();
    private Set<String> papeisAtuais = Set.of("ROLE_ADMIN");
    private BearerTokenAuthenticationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        IAccessTokenReader leitor = token -> {
            lidos.add(token);
            return TOKEN_VALIDO.equals(token) ? Optional.of(PORTADOR) : Optional.empty();
        };
        ICurrentRolesReader papeis = id -> PORTADOR.id().equals(id) ? papeisAtuais : Set.of();
        filter = new BearerTokenAuthenticationFilter(leitor, papeis);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bearer aceito pelo leitor popula o contexto com o portador e seus papéis")
    void deveAutenticarComTokenValido() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALIDO);

        filter.doFilter(request, response, chain);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(autenticacao.getPrincipal() instanceof AuthenticatedUser);
        assertEquals("joao.silva", autenticacao.getName());
        assertEquals(List.of("ROLE_ADMIN"),
                autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertEquals(List.of(TOKEN_VALIDO), lidos, "o leitor recebe só o que vem depois de 'Bearer '");
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Os papéis vêm do cadastro agora, não do token: perfil removido deixa de autorizar na hora")
    void deveUsarOsPapeisAtuaisEmVezDosDoToken() throws Exception {
        papeisAtuais = Set.of("ROLE_CLIENT");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALIDO);

        filter.doFilter(request, response, chain);

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(List.of("ROLE_CLIENT"),
                autenticacao.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        assertEquals(Set.of("ROLE_CLIENT"), ((AuthenticatedUser) autenticacao.getPrincipal()).roles());
    }

    @Test
    @DisplayName("Usuário que não existe mais segue autenticado pelo token, mas sem papel nenhum")
    void deveAutenticarSemPapelQuemNaoExisteMais() throws Exception {
        papeisAtuais = Set.of();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_VALIDO);

        filter.doFilter(request, response, chain);

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Sem cabeçalho, a requisição segue anônima e o leitor nem é consultado")
    void deveSeguirSemCabecalho() throws Exception {
        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(lidos.isEmpty());
        verify(chain).doFilter(request, response);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Basic dXNlcjpzZW5oYQ==", "bearer minusculo", "Token abc", ""})
    @DisplayName("Cabeçalho com outro esquema é ignorado")
    void deveIgnorarOutroEsquema(String header) throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, header);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(lidos.isEmpty());
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Token recusado pelo leitor não autentica, e a requisição segue anônima")
    void deveSeguirAnonimoComTokenInvalido() throws Exception {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer token-que-nao-vale");

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(request, response);
    }
}
