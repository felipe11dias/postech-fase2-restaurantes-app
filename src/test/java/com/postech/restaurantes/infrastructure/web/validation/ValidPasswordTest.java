package com.postech.restaurantes.infrastructure.web.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.postech.restaurantes.infrastructure.web.auth.ResetPasswordRequest;
import com.postech.restaurantes.infrastructure.web.user.ChangePasswordRequest;
import com.postech.restaurantes.infrastructure.web.user.NewUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A regra é conferida pelo motor de Bean Validation de verdade, aplicado aos records HTTP —
 * sem contexto Spring. Assim o teste prova não só o validador, mas que a anotação está de fato
 * nos três campos de senha nova.
 */
class ValidPasswordTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    /** 40 caracteres, 80 bytes em UTF-8: passaria em {@code @Size(max = 72)}. */
    private static final String ACENTUADA_80_BYTES = "ç".repeat(40);

    @BeforeAll
    static void abrirValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidador() {
        factory.close();
    }

    private final ValidPasswordValidator regra = new ValidPasswordValidator();

    @Nested
    @DisplayName("Regra")
    class Regra {

        @Test
        @DisplayName("A senha acentuada de 80 bytes cabe em 72 caracteres — é esse o caso que a regra existe para pegar")
        void devePremissaDoCasoSerVerdadeira() {
            assertTrue(ACENTUADA_80_BYTES.length() <= 72);
            assertEquals(80, ACENTUADA_80_BYTES.getBytes(StandardCharsets.UTF_8).length);
        }

        @Test
        @DisplayName("Recusa senha que passa de 72 bytes, mesmo com menos de 72 caracteres")
        void deveRecusarAcimaDe72Bytes() {
            assertFalse(regra.isValid(ACENTUADA_80_BYTES, null));
        }

        @Test
        @DisplayName("Aceita exatamente 72 bytes e recusa 73")
        void deveRespeitarOLimiteExato() {
            assertTrue(regra.isValid("a".repeat(72), null));
            assertFalse(regra.isValid("a".repeat(73), null));
        }

        @Test
        @DisplayName("Aceita senha acentuada que cabe em 72 bytes")
        void deveAceitarAcentuadaDentroDoLimite() {
            assertTrue(regra.isValid("ç".repeat(36), null));
        }

        @Test
        @DisplayName("Recusa menos de 8 caracteres e aceita exatamente 8")
        void deveRespeitarOMinimo() {
            assertFalse(regra.isValid("1234567", null));
            assertTrue(regra.isValid("12345678", null));
        }

        @Test
        @DisplayName("O mínimo conta caracteres, não unidades UTF-16: 8 emojis são 8 caracteres")
        void deveContarPontosDeCodigo() {
            String quatroEmojis = "🔒".repeat(4);

            assertEquals(8, quatroEmojis.length(), "4 emojis ocupam 8 unidades UTF-16");
            assertFalse(regra.isValid(quatroEmojis, null), "mas são só 4 caracteres");
        }

        @Test
        @DisplayName("Nulo é deixado para o @NotBlank")
        void deveIgnorarNulo() {
            assertTrue(regra.isValid(null, null));
        }
    }

    @Nested
    @DisplayName("Aplicação nos corpos HTTP")
    class Aplicacao {

        @ParameterizedTest
        @ValueSource(strings = {"senhaSegura123", "çãõéíúâêô1234"})
        @DisplayName("Cadastro aceita senha válida, com ou sem acento")
        void deveAceitarNoCadastro(String senha) {
            assertTrue(violacoesDeSenha(new NewUserRequest("João", "joao@email.com", "joao", senha,
                    Set.of("ROLE_CUSTOMER"), null)).isEmpty());
        }

        @Test
        @DisplayName("Cadastro, troca e redefinição recusam a senha de 80 bytes na borda")
        void deveRecusarNosTresCorpos() {
            assertEquals(1, violacoesDeSenha(new NewUserRequest("João", "joao@email.com", "joao",
                    ACENTUADA_80_BYTES, Set.of("ROLE_CUSTOMER"), null)).size());
            assertEquals(1, violacoesDeSenha(new ChangePasswordRequest("atual", ACENTUADA_80_BYTES,
                    ACENTUADA_80_BYTES)).size());
            assertEquals(1, violacoesDeSenha(new ResetPasswordRequest("token", ACENTUADA_80_BYTES,
                    ACENTUADA_80_BYTES)).size());
        }

        @Test
        @DisplayName("A mensagem explica o limite em bytes, para o usuário entender a recusa")
        void deveExplicarOLimite() {
            String mensagem = violacoesDeSenha(new ChangePasswordRequest("atual", ACENTUADA_80_BYTES,
                    ACENTUADA_80_BYTES)).iterator().next().getMessage();

            assertTrue(mensagem.contains("72 bytes"));
        }

        private static <T> Set<ConstraintViolation<T>> violacoesDeSenha(T corpo) {
            return validator.validate(corpo).stream()
                    .filter(v -> v.getConstraintDescriptor().getAnnotation() instanceof ValidPassword)
                    .collect(java.util.stream.Collectors.toSet());
        }
    }
}
