package com.postech.restaurantes.infrastructure.web.error;

import com.postech.restaurantes.domain.exception.DuplicateResourceException;
import com.postech.restaurantes.domain.exception.ForbiddenOperationException;
import com.postech.restaurantes.domain.exception.InvalidCredentialsException;
import com.postech.restaurantes.domain.exception.InvalidOrExpiredTokenException;
import com.postech.restaurantes.domain.exception.InvalidPasswordException;
import com.postech.restaurantes.domain.exception.InvariantViolationException;
import com.postech.restaurantes.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Tradução de exceção para HTTP — e o <strong>único</strong> lugar que a faz. O núcleo lança
 * exceções de domínio sem saber que existe HTTP; os controllers não capturam nada. Todo
 * mapeamento de "o que deu errado" para "que status e que corpo" mora aqui.
 *
 * <p>Princípio: <strong>resposta vaga para o cliente, registro detalhado para quem
 * opera</strong>. Vai para a resposta apenas mensagem escrita para o usuário — a das exceções
 * de domínio e a das violações de invariante. Mensagem de biblioteca, de parser ou de exceção
 * inesperada nunca sai: ela descreve a implementação, e só interessa a quem lê o log.
 *
 * <p>Estende {@link ResponseEntityExceptionHandler} para que as exceções do próprio Spring MVC
 * (método não suportado, rota inexistente, tipo de mídia errado) também saiam como
 * {@link ProblemDetail}, em vez de caírem no tratamento genérico como erro 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String ERRORS = "errors";
    static final String INVALID_FIELDS = "Um ou mais campos são inválidos.";
    static final String UNREADABLE_BODY = "O corpo da requisição está ausente ou malformado.";
    static final String INVALID_DATA = "Um ou mais dados enviados são inválidos.";
    static final String ACCESS_DENIED = "Você não tem permissão para acessar este recurso.";
    static final String CONFLICT = "Os dados enviados conflitam com um registro existente.";
    static final String UNEXPECTED = "Ocorreu um erro inesperado. Tente novamente mais tarde.";

    private final ProblemDetailFactory problems;

    public GlobalExceptionHandler(ProblemDetailFactory problems) {
        this.problems = problems;
    }

    // --- Violações de invariante e dados inválidos ------------------------------------------

    /** A mensagem do {@code Guard} é escrita para o usuário: vai para a resposta. */
    @ExceptionHandler(InvariantViolationException.class)
    public ResponseEntity<ProblemDetail> handleInvariantViolation(InvariantViolationException ex) {
        return respond(ProblemType.INVALID_REQUEST, ex.getMessage());
    }

    /**
     * Qualquer outra {@code IllegalArgumentException} vem de biblioteca ou de código técnico, e a
     * mensagem descreve a implementação. O status é o mesmo — o dado de entrada foi recusado —,
     * mas o detalhe é genérico e a exceção inteira vai para o log.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
        LOG.warn("Argumento recusado fora das invariantes do domínio", ex);
        return respond(ProblemType.INVALID_REQUEST, INVALID_DATA);
    }

    // --- Exceções de domínio: a mensagem é regra de negócio, escrita para o usuário ---------

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ProblemDetail> handleInvalidPassword(InvalidPasswordException ex) {
        return respond(ProblemType.INVALID_PASSWORD, ex.getMessage());
    }

    @ExceptionHandler(InvalidOrExpiredTokenException.class)
    public ResponseEntity<ProblemDetail> handleInvalidToken(InvalidOrExpiredTokenException ex) {
        return respond(ProblemType.INVALID_TOKEN, ex.getMessage());
    }

    /** A mensagem é a mesma para login inexistente e senha errada — decisão do caso de uso. */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(InvalidCredentialsException ex) {
        return respond(ProblemType.AUTHENTICATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ProblemDetail> handleForbiddenOperation(ForbiddenOperationException ex) {
        return respond(ProblemType.FORBIDDEN_OPERATION, ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex) {
        return respond(ProblemType.RESOURCE_NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateResourceException ex) {
        return respond(ProblemType.DATA_CONFLICT, ex.getMessage());
    }

    // --- Segurança e persistência -----------------------------------------------------------

    /**
     * Recusa do {@code @PreAuthorize} (recurso de outro usuário, operação administrativa). Sem
     * este mapeamento, ela cairia no tratamento genérico e viraria 500.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return respond(ProblemType.ACCESS_DENIED, ACCESS_DENIED);
    }

    /**
     * O caso de uso confere e-mail e login únicos antes de gravar, mas duas requisições
     * simultâneas podem passar pela conferência juntas; quem decide o empate é a restrição
     * única do banco. É o mesmo conflito, e recebe o mesmo 409 — sem o nome da restrição.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex) {
        LOG.warn("Restrição de integridade violada na gravação", ex);
        return respond(ProblemType.DATA_CONFLICT, CONFLICT);
    }

    /** O imprevisto: resposta genérica, exceção inteira no log em ERROR. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        LOG.error("Erro inesperado ao processar a requisição", ex);
        return respond(ProblemType.UNEXPECTED_ERROR, UNEXPECTED);
    }

    // --- Exceções do Spring MVC -------------------------------------------------------------

    /** Bean Validation: um mapa {@code errors} com as mensagens de cada campo recusado. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problems.create(ProblemType.INVALID_REQUEST, INVALID_FIELDS);
        problem.setProperty(ERRORS, errorsByField(ex.getBindingResult().getFieldErrors()));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** A mensagem do parser cita classes e posições do JSON: não sai. */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problems.create(ProblemType.INVALID_REQUEST, UNREADABLE_BODY);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /**
     * Parâmetro com formato errado — tipicamente um {@code {id}} que não é UUID. O nome do
     * parâmetro é do nosso código e pode sair; o valor enviado, não precisa voltar.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String detail = ex.getPropertyName() == null
                ? INVALID_DATA
                : "O parâmetro '" + ex.getPropertyName() + "' tem formato inválido.";
        ProblemDetail problem = problems.create(ProblemType.INVALID_REQUEST, detail);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    /** Último passo de toda resposta montada pela superclasse: garante o {@code timestamp}. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            problems.stamp(problem);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<ProblemDetail> respond(ProblemType type, String detail) {
        return ResponseEntity.status(type.status()).body(problems.create(type, detail));
    }

    /**
     * Campo → mensagens, em ordem estável. Um campo pode violar mais de uma restrição ao mesmo
     * tempo (senha vazia é "em branco" e "curta"), e a ordem das violações não é garantida.
     */
    static Map<String, List<String>> errorsByField(List<FieldError> fieldErrors) {
        return fieldErrors.stream().collect(Collectors.groupingBy(
                FieldError::getField,
                TreeMap::new,
                Collectors.collectingAndThen(
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList()),
                        messages -> messages.stream().sorted().toList())));
    }
}
