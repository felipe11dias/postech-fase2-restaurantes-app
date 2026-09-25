package com.postech.restaurantes.infrastructure.web.error;

import static com.postech.restaurantes.infrastructure.web.error.ProblemDetailFactoryTest.AGORA;
import static com.postech.restaurantes.infrastructure.web.error.ProblemDetailFactoryTest.RELOGIO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.InvariantViolationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * O handler é testado como objeto: cada exceção entra, e o que se verifica é o status, a
 * categoria e — o ponto mais importante — o que vai e o que não vai para o detalhe.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(new ProblemDetailFactory(RELOGIO));
    private final WebRequest request =
            new ServletWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse());

    /** Alvo para montar o {@code MethodParameter} das exceções de binding. */
    @SuppressWarnings("unused")
    void alvo(String corpo) {
    }

    private MethodParameter parametro() throws NoSuchMethodException {
        return new MethodParameter(getClass().getDeclaredMethod("alvo", String.class), 0);
    }

    private static ProblemDetail problema(ResponseEntity<?> resposta) {
        return (ProblemDetail) resposta.getBody();
    }

    @Nested
    @DisplayName("Exceções de domínio: a mensagem é regra de negócio e vai para o cliente")
    class Dominio {

        static Stream<Arguments> casos() {
            return Stream.of(
                    Arguments.of(new InvariantViolationException("CEP inválido"), ProblemType.INVALID_REQUEST),
                    Arguments.of(new InvalidPasswordException("Senha atual incorreta"), ProblemType.INVALID_PASSWORD),
                    Arguments.of(new InvalidOrExpiredTokenException("Token inválido"), ProblemType.INVALID_TOKEN),
                    Arguments.of(new InvalidCredentialsException("Login ou senha inválidos"),
                            ProblemType.AUTHENTICATION_FAILED),
                    Arguments.of(new ForbiddenOperationException("ROLE_ADMIN não pode ser solicitado"),
                            ProblemType.FORBIDDEN_OPERATION),
                    Arguments.of(new ResourceNotFoundException("Usuário não encontrado"),
                            ProblemType.RESOURCE_NOT_FOUND),
                    Arguments.of(new DuplicateResourceException("E-mail já cadastrado"), ProblemType.DATA_CONFLICT));
        }

        @ParameterizedTest(name = "{1}")
        @MethodSource("casos")
        @DisplayName("Cada exceção de domínio vira a sua categoria, com a mensagem original")
        void deveTraduzirComAMensagem(RuntimeException ex, ProblemType esperado) {
            ResponseEntity<ProblemDetail> resposta = traduzir(ex);

            assertEquals(esperado.status(), resposta.getStatusCode());
            assertEquals(esperado.type(), problema(resposta).getType());
            assertEquals(esperado.title(), problema(resposta).getTitle());
            assertEquals(ex.getMessage(), problema(resposta).getDetail());
            assertEquals(AGORA, problema(resposta).getProperties().get(ProblemDetailFactory.TIMESTAMP));
        }

        private ResponseEntity<ProblemDetail> traduzir(RuntimeException ex) {
            Map<Class<?>, Function<RuntimeException, ResponseEntity<ProblemDetail>>> rotas = Map.of(
                    InvariantViolationException.class,
                    e -> handler.handleInvariantViolation((InvariantViolationException) e),
                    InvalidPasswordException.class, e -> handler.handleInvalidPassword((InvalidPasswordException) e),
                    InvalidOrExpiredTokenException.class,
                    e -> handler.handleInvalidToken((InvalidOrExpiredTokenException) e),
                    InvalidCredentialsException.class,
                    e -> handler.handleInvalidCredentials((InvalidCredentialsException) e),
                    ForbiddenOperationException.class,
                    e -> handler.handleForbiddenOperation((ForbiddenOperationException) e),
                    ResourceNotFoundException.class, e -> handler.handleNotFound((ResourceNotFoundException) e),
                    DuplicateResourceException.class, e -> handler.handleDuplicate((DuplicateResourceException) e));
            return rotas.get(ex.getClass()).apply(ex);
        }
    }

    @Nested
    @DisplayName("Mensagens internas nunca saem")
    class MensagensInternas {

        @Test
        @DisplayName("IllegalArgumentException de biblioteca vira 400 com detalhe genérico")
        void deveEsconderMensagemDeBiblioteca() {
            ResponseEntity<ProblemDetail> resposta =
                    handler.handleIllegalArgument(new IllegalArgumentException("password cannot be more than 72 bytes"));

            assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
            assertEquals(GlobalExceptionHandler.INVALID_DATA, problema(resposta).getDetail());
        }

        @Test
        @DisplayName("Violação de integridade no banco vira 409 sem o nome da restrição")
        void deveEsconderARestricaoDoBanco() {
            ResponseEntity<ProblemDetail> resposta = handler.handleDataIntegrity(
                    new DataIntegrityViolationException("duplicate key value violates unique constraint users_email_key"));

            assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
            assertEquals(ProblemType.DATA_CONFLICT.type(), problema(resposta).getType());
            assertFalse(problema(resposta).getDetail().contains("users_email_key"));
        }

        @Test
        @DisplayName("Erro inesperado vira 500 genérico, sem a mensagem nem a classe da exceção")
        void deveEsconderOErroInesperado() {
            ResponseEntity<ProblemDetail> resposta =
                    handler.handleUnexpected(new IllegalStateException("Usuário inexistente para atualização: 123"));

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resposta.getStatusCode());
            assertEquals(GlobalExceptionHandler.UNEXPECTED, problema(resposta).getDetail());
            assertFalse(problema(resposta).toString().contains("IllegalStateException"));
        }

        @Test
        @DisplayName("Recusa do @PreAuthorize vira 403 com detalhe fixo")
        void deveTraduzirAcessoNegado() {
            ResponseEntity<ProblemDetail> resposta =
                    handler.handleAccessDenied(new AuthorizationDeniedException("Access Denied"));

            assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
            assertEquals(ProblemType.ACCESS_DENIED.type(), problema(resposta).getType());
            assertEquals(GlobalExceptionHandler.ACCESS_DENIED, problema(resposta).getDetail());
        }
    }

    @Nested
    @DisplayName("Exceções do Spring MVC")
    class SpringMvc {

        @Test
        @DisplayName("Bean Validation devolve o mapa errors, campo a campo, em ordem estável")
        void deveMapearOsErrosPorCampo() throws NoSuchMethodException {
            BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "corpo");
            resultado.addError(new FieldError("corpo", "password", "muito curta"));
            resultado.addError(new FieldError("corpo", "password", "em branco"));
            resultado.addError(new FieldError("corpo", "email", "e-mail inválido"));

            ResponseEntity<Object> resposta = handler.handleMethodArgumentNotValid(
                    new MethodArgumentNotValidException(parametro(), resultado), new HttpHeaders(),
                    HttpStatus.BAD_REQUEST, request);

            ProblemDetail problem = problema(resposta);
            assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
            assertEquals(GlobalExceptionHandler.INVALID_FIELDS, problem.getDetail());
            assertEquals(Map.of("email", List.of("e-mail inválido"), "password", List.of("em branco", "muito curta")),
                    problem.getProperties().get(GlobalExceptionHandler.ERRORS));
            assertEquals(List.of("email", "password"), List.copyOf(
                    ((Map<?, ?>) problem.getProperties().get(GlobalExceptionHandler.ERRORS)).keySet()));
        }

        @Test
        @DisplayName("Corpo malformado vira 400 sem a mensagem do parser")
        void deveEsconderAMensagemDoParser() {
            ResponseEntity<Object> resposta = handler.handleHttpMessageNotReadable(
                    new HttpMessageNotReadableException("Unexpected character ('}' (code 125)) at line 1",
                            new MockHttpInputMessage(new byte[0])),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

            assertEquals(GlobalExceptionHandler.UNREADABLE_BODY, problema(resposta).getDetail());
        }

        @Test
        @DisplayName("Parâmetro de formato errado cita o nome do parâmetro, não o valor recebido")
        void deveCitarOParametro() throws NoSuchMethodException {
            ResponseEntity<Object> resposta = handler.handleTypeMismatch(
                    new MethodArgumentTypeMismatchException("nao-e-uuid", UUID.class, "id", parametro(), null),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

            assertEquals("O parâmetro 'id' tem formato inválido.", problema(resposta).getDetail());
            assertFalse(problema(resposta).getDetail().contains("nao-e-uuid"));
        }

        @Test
        @DisplayName("Sem o nome do parâmetro, o detalhe é genérico")
        void deveUsarDetalheGenericoSemNome() {
            ResponseEntity<Object> resposta = handler.handleTypeMismatch(new TypeMismatchException("x", UUID.class),
                    new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

            assertEquals(GlobalExceptionHandler.INVALID_DATA, problema(resposta).getDetail());
        }

        @Test
        @DisplayName("Exceção que o Spring já traduz sozinho também ganha o instante")
        void deveCarimbarAsRespostasPadraoDoSpring() throws Exception {
            ResponseEntity<Object> resposta =
                    handler.handleException(new HttpRequestMethodNotSupportedException("PATCH"), request);

            assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resposta.getStatusCode());
            assertEquals(AGORA, problema(resposta).getProperties().get(ProblemDetailFactory.TIMESTAMP));
        }

        @Test
        @DisplayName("Corpo que não é ProblemDetail passa sem alteração")
        void deveDeixarPassarCorpoDeOutroTipo() {
            ResponseEntity<Object> resposta =
                    handler.createResponseEntity(null, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);

            assertNull(resposta.getBody());
            assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        }
    }
}
