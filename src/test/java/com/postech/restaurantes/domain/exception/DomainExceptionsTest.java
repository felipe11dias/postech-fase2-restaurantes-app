package com.postech.restaurantes.domain.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DomainExceptionsTest {

    static Stream<Arguments> excecoes() {
        return Stream.of(
                Arguments.of("ResourceNotFoundException", (Function<String, DomainException>) ResourceNotFoundException::new),
                Arguments.of("DuplicateResourceException", (Function<String, DomainException>) DuplicateResourceException::new),
                Arguments.of("InvalidPasswordException", (Function<String, DomainException>) InvalidPasswordException::new),
                Arguments.of("ForbiddenOperationException", (Function<String, DomainException>) ForbiddenOperationException::new),
                Arguments.of("InvalidOrExpiredTokenException", (Function<String, DomainException>) InvalidOrExpiredTokenException::new),
                Arguments.of("InvalidCredentialsException", (Function<String, DomainException>) InvalidCredentialsException::new));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("excecoes")
    @DisplayName("Toda exceção de domínio é não verificada e preserva a mensagem")
    void devePreservarMensagemESerRuntime(String nome, Function<String, DomainException> fabrica) {
        DomainException ex = fabrica.apply("mensagem de " + nome);

        assertInstanceOf(RuntimeException.class, ex);
        assertEquals("mensagem de " + nome, ex.getMessage());
    }
}
