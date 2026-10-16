package com.postech.restaurantes.infrastructure.api.rest.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedActorTest {

    private final AuthenticatedActor actor = new AuthenticatedActor();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sem ninguém autenticado, não há autor")
    void naoDeveHaverAutorSemAutenticacao() {
        assertTrue(actor.currentLogin().isEmpty());
    }

    @Test
    @DisplayName("Requisição anônima não tem autor")
    void naoDeveHaverAutorParaAnonimo() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertTrue(actor.currentLogin().isEmpty());
    }

    @Test
    @DisplayName("Autenticação ainda não confirmada não vira autor")
    void naoDeveHaverAutorParaAutenticacaoNaoConfirmada() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("joao.silva", "senha"));

        assertTrue(actor.currentLogin().isEmpty());
    }

    @Test
    @DisplayName("Usuário autenticado é identificado pelo login")
    void deveIdentificarOLoginAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "joao.silva", "senha", List.of(new SimpleGrantedAuthority("ROLE_CLIENT"))));

        assertEquals(Optional.of("joao.silva"), actor.currentLogin());
    }
}
