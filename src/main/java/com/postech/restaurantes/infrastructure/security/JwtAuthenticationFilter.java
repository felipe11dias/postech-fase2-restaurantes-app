package com.postech.restaurantes.infrastructure.security;

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
 * Traduz o cabeçalho {@code Authorization: Bearer <jwt>} em contexto de segurança.
 *
 * <p>Não decide nada: se o token não vale, a requisição segue <strong>anônima</strong> e quem
 * recusa é a configuração de autorização. Assim a regra de "o que exige autenticação" fica em
 * um lugar só, e não espalhada entre filtro e configuração.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenIssuer tokenIssuer;

    public JwtAuthenticationFilter(JwtTokenIssuer tokenIssuer) {
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        extrairToken(request)
                .flatMap(tokenIssuer::read)
                .ifPresent(JwtAuthenticationFilter::autenticar);
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
