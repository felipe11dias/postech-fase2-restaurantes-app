package com.postech.restaurantes.infrastructure.web.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Contêiner de {@link ErrorResponse} repetida; não é usada diretamente. */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ErrorResponses {

    ErrorResponse[] value();
}
