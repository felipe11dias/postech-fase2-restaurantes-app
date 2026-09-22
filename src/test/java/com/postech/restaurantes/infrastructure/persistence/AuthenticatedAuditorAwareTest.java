package com.postech.restaurantes.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthenticatedAuditorAwareTest {

    private final AuthenticatedAuditorAware auditor = new AuthenticatedAuditorAware();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sem ninguém autenticado, o autor registrado é o sistema")
    void deveRegistrarSistemaSemAutenticacao() {
        assertEquals(AuthenticatedAuditorAware.SYSTEM, auditor.getCurrentAuditor().orElseThrow());
    }

    @Test
    @DisplayName("Requisição anônima também é registrada como sistema")
    void deveRegistrarSistemaParaAnonimo() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "chave", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertEquals(AuthenticatedAuditorAware.SYSTEM, auditor.getCurrentAuditor().orElseThrow());
    }

    @Test
    @DisplayName("Autenticação ainda não confirmada não vira autor")
    void deveRegistrarSistemaParaAutenticacaoNaoConfirmada() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("joao.silva", "senha"));

        assertEquals(AuthenticatedAuditorAware.SYSTEM, auditor.getCurrentAuditor().orElseThrow());
    }

    @Test
    @DisplayName("Usuário autenticado é registrado pelo login")
    void deveRegistrarOLoginAutenticado() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "joao.silva", "senha", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));

        assertEquals("joao.silva", auditor.getCurrentAuditor().orElseThrow());
    }
}
