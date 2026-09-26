package com.postech.restaurantes.infrastructure.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Senha nova aceitável: ao menos {@value ValidPasswordValidator#MIN_CHARACTERS} caracteres e no
 * máximo {@value ValidPasswordValidator#MAX_BYTES} <strong>bytes</strong> em UTF-8.
 *
 * <p>Existe porque {@code @Size(max = 72)} conta caracteres, e o limite do BCrypt é em bytes:
 * uma senha de 40 caracteres acentuados ocupa 80 bytes, passaria no {@code @Size} e faria o
 * codificador falhar lá dentro. O limite é detalhe do algoritmo e fica aqui, na borda da
 * infraestrutura, recusando cedo e com explicação.
 *
 * <p>Nulo é considerado válido: ausência é assunto do {@code @NotBlank}, que acompanha esta
 * anotação — cada restrição responde por uma coisa só.
 */
@Documented
@Constraint(validatedBy = ValidPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Senha deve ter ao menos 8 caracteres e no máximo 72 bytes "
            + "(letras acentuadas ocupam mais de um byte cada)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
