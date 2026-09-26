package com.postech.restaurantes.infrastructure.web.doc;

import com.postech.restaurantes.infrastructure.web.error.ProblemType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * Converte cada {@link ErrorResponse} de uma operação em resposta documentada: código vindo da
 * categoria, esquema ProblemDetail e um exemplo com o {@code type}, o {@code title} e o
 * {@code status} reais daquela categoria.
 *
 * <p>Casos com o mesmo código (a troca de senha responde 400 tanto por senha atual incorreta
 * quanto por senha nova inválida) viram uma resposta só: as descrições se juntam e cada caso
 * ganha o seu exemplo nomeado — o OpenAPI tem uma entrada por código, e perder um dos casos
 * seria documentar menos do que a API faz.
 */
@Component
public class ErrorResponseOperationCustomizer implements OperationCustomizer {

    static final String EXAMPLE_ID = "7295577e-afe6-4875-8bbf-d21c21860711";
    static final String EXAMPLE_TIMESTAMP = "2026-09-25T17:10:42.118";

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ErrorResponse[] errors = handlerMethod.getMethod().getAnnotationsByType(ErrorResponse.class);
        String path = requestPath(handlerMethod);
        Map<Integer, List<ErrorResponse>> byStatus = Arrays.stream(errors).collect(
                Collectors.groupingBy(error -> error.type().status().value(), TreeMap::new, Collectors.toList()));
        byStatus.forEach((status, cases) ->
                operation.getResponses().addApiResponse(String.valueOf(status), response(cases, path)));
        return operation;
    }

    private static ApiResponse response(List<ErrorResponse> cases, String path) {
        MediaType mediaType = new MediaType().schema(new Schema<>().$ref(ProblemDetailOpenApiCustomizer.SCHEMA_REF));
        cases.forEach(error -> mediaType.addExamples(error.description(),
                new Example().summary(error.description()).value(example(error, path))));
        return new ApiResponse()
                .description(cases.stream().map(ErrorResponse::description).collect(Collectors.joining("; ")))
                .content(new Content().addMediaType(ProblemDetailOpenApiCustomizer.PROBLEM_JSON, mediaType));
    }

    static Map<String, Object> example(ErrorResponse error, String path) {
        ProblemType type = error.type();
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("type", type.type().toString());
        example.put("title", type.title());
        example.put("status", type.status().value());
        example.put("detail", error.description());
        example.put("instance", path.replace("{id}", EXAMPLE_ID));
        example.put("timestamp", EXAMPLE_TIMESTAMP);
        return example;
    }

    /** Caminho da operação — prefixo da classe mais o do método — para o {@code instance}. */
    static String requestPath(HandlerMethod handlerMethod) {
        return firstPath(AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequestMapping.class))
                + firstPath(AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RequestMapping.class));
    }

    private static String firstPath(RequestMapping mapping) {
        return mapping == null || mapping.path().length == 0 ? "" : mapping.path()[0];
    }
}
