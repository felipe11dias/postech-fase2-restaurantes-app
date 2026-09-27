package com.postech.restaurantes.infrastructure.persistence.jpa.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Sem Spring Security: o autor chega pronto, e o teste só precisa de um Supplier. */
class AuthenticatedAuditorAwareTest {

    @Test
    @DisplayName("Sem autor corrente, o autor registrado é o sistema")
    void deveRegistrarSistemaQuandoNaoHaAutor() {
        AuthenticatedAuditorAware auditor = new AuthenticatedAuditorAware(Optional::empty);

        assertEquals(AuthenticatedAuditorAware.SYSTEM, auditor.getCurrentAuditor().orElseThrow());
    }

    @Test
    @DisplayName("Com autor corrente, o autor registrado é o login dele")
    void deveRegistrarOLoginQuandoHaAutor() {
        AuthenticatedAuditorAware auditor = new AuthenticatedAuditorAware(() -> Optional.of("joao.silva"));

        assertEquals("joao.silva", auditor.getCurrentAuditor().orElseThrow());
    }

    @Test
    @DisplayName("A fonte do autor é obrigatória")
    void naoDeveAceitarFonteNula() {
        assertThrows(NullPointerException.class, () -> new AuthenticatedAuditorAware(null));
    }
}
