package com.postech.restaurantes.infrastructure.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Traduz o cabeçalho {@code Authorization: Bearer <token>} em contexto de segurança. Não sabe o
 * formato do token: quem o lê é o {@link IAccessTokenReader}, implementado pelo módulo de token.
 *
 * <p>Não decide nada: se o token não vale, a requisição segue <strong>anônima</strong> e quem
 * recusa é a configuração de autorização. Assim a regra de "o que exige autenticação" fica em
 * um lugar só, e não espalhada entre filtro e configuração.
 */
@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";

    private final IAccessTokenReader tokenReader;

    public BearerTokenAuthenticationFilter(IAccessTokenReader tokenReader) {
        this.tokenReader = tokenReader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        extrairToken(request)
                .flatMap(tokenReader::read)
                .ifPresent(BearerTokenAuthenticationFilter::autenticar);
        chain.doFilter(request, response);
    }

    private static java.util.Optional<String> extrairToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(header.substring(BEARER_PREFIX.length()));
    }

    /** O principal é o próprio {@link AuthenticatedUser}: a regra de posse precisa do id. */
    private static void autenticar(AuthenticatedUser user) {
        List<SimpleGrantedAuthority> autoridades = user.roles().stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, autoridades));
    }
}
