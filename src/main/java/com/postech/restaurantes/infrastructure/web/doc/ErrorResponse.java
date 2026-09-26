package com.postech.restaurantes.infrastructure.web.doc;

import com.postech.restaurantes.infrastructure.web.error.ProblemType;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documenta um caso de erro de uma operação pela sua <strong>categoria</strong>, e não pelo
 * código HTTP.
 *
 * <p>O código documentado sai de {@link ProblemType#status()} — o mesmo catálogo que o
 * {@code GlobalExceptionHandler} usa para responder. Por isso documentação e comportamento não
 * podem divergir: não existe um segundo lugar onde "acesso negado é 403" esteja escrito. Com um
 * {@code @ApiResponse(responseCode = "403")} solto, esse número seria uma cópia, e cópia
 * envelhece.
 *
 * <p>O {@link ErrorResponseOperationCustomizer} transforma cada anotação em resposta
 * ProblemDetail com um exemplo da categoria; casos com o mesmo código viram uma resposta só,
 * com um exemplo nomeado para cada um. No exemplo, o {@code detail} é a própria descrição do
 * caso — ilustrativo, não a mensagem literal.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ErrorResponses.class)
public @interface ErrorResponse {

    ProblemType type();

    /** Em que situação a operação responde com esta categoria. */
    String description();
}
