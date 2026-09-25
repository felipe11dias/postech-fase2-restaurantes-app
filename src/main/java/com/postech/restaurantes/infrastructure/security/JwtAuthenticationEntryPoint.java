package com.postech.restaurantes.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.postech.restaurantes.infrastructure.web.error.ProblemDetailFactory;
import com.postech.restaurantes.infrastructure.web.error.ProblemType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * O {@code 401} de quem chega sem credencial válida, no mesmo formato {@link ProblemDetail} de
 * todos os outros erros.
 *
 * <p>Precisa existir porque a recusa acontece na cadeia de filtros de segurança, antes de a
 * requisição chegar ao Spring MVC — e o {@code GlobalExceptionHandler} só enxerga o que
 * acontece dentro dele. Sem isto, o 401 seria a única resposta de erro sem corpo.
 *
 * <p>O detalhe é o mesmo para token ausente, expirado ou adulterado: diferenciar os casos só
 * ajudaria quem está sondando a API.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String DETAIL = "Autenticação necessária: envie um token Bearer válido.";

    private final ProblemDetailFactory problems;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ProblemDetailFactory problems, ObjectMapper objectMapper) {
        this.problems = problems;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemDetail problem = problems.create(ProblemType.UNAUTHENTICATED, DETAIL, request.getRequestURI());
        response.setStatus(ProblemType.UNAUTHENTICATED.status().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
