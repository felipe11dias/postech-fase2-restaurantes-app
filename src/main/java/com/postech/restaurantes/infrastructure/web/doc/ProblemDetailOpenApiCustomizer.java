package com.postech.restaurantes.infrastructure.web.doc;

import com.postech.restaurantes.infrastructure.web.error.ProblemType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.stereotype.Component;

/**
 * Documenta, uma vez só, que <strong>toda resposta de erro é ProblemDetail</strong> — a mesma
 * regra que o {@code GlobalExceptionHandler} aplica em tempo de execução, agora na
 * documentação.
 *
 * <p>Registra o esquema {@code ProblemDetail}, montado aqui e não gerado da classe do Spring: em
 * tempo de execução as propriedades extras ({@code timestamp}, {@code errors}) saem achatadas no
 * corpo, e a classe as guarda num mapa — gerado a partir dela, o esquema descreveria um formato
 * que a API não produz.
 *
 * <p>Também é a rede de segurança da regra: os erros declarados com {@link ErrorResponse} já
 * chegam como ProblemDetail e ficam como estão; um erro declarado de outro jeito — um
 * {@code @ApiResponse} solto — tem o conteúdo corrigido, e nunca fica documentado com o formato
 * do tipo de retorno da operação.
 */
@Component
public class ProblemDetailOpenApiCustomizer implements OpenApiCustomizer {

    static final String PROBLEM_JSON = org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    static final String SCHEMA_REF = "#/components/schemas/" + ApiDocumentation.PROBLEM_DETAIL_SCHEMA;
    static final String ABOUT_BLANK = "about:blank";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents().addSchemas(ApiDocumentation.PROBLEM_DETAIL_SCHEMA, problemDetailSchema());
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .flatMap(operation -> operation.getResponses().entrySet().stream())
                .filter(response -> isError(response.getKey()))
                .filter(response -> !isProblemDetail(response.getValue().getContent()))
                .forEach(response -> response.getValue().setContent(problemContent()));
    }

    static boolean isError(String responseCode) {
        return responseCode.startsWith("4") || responseCode.startsWith("5");
    }

    /**
     * Resposta já documentada como ProblemDetail — as de {@link ErrorResponse}, com exemplo por
     * categoria — fica como está. Só a declarada de outro jeito é corrigida.
     */
    static boolean isProblemDetail(Content content) {
        return content != null && content.containsKey(PROBLEM_JSON);
    }

    private static Content problemContent() {
        return new Content().addMediaType(PROBLEM_JSON, new MediaType().schema(new Schema<>().$ref(SCHEMA_REF)));
    }

    /**
     * O {@code type} é enumerado a partir do próprio catálogo {@link ProblemType}: a lista de
     * categorias documentada é a mesma que o handler usa, sem cópia. {@code about:blank} é o
     * {@code type} das respostas que o Spring monta sozinho (405, 415, rota inexistente).
     */
    static Schema<String> typeSchema() {
        StringSchema schema = new StringSchema();
        schema.format("uri").description("Identificador da categoria do erro — é nele que o cliente deve se apoiar.");
        Arrays.stream(ProblemType.values()).map(type -> type.type().toString()).forEach(schema::addEnumItem);
        schema.addEnumItem(ABOUT_BLANK);
        return schema;
    }

    static Schema<?> problemDetailSchema() {
        Schema<?> schema = new ObjectSchema()
                .description("Formato de toda resposta de erro da API (RFC 9457).")
                .addProperty("type", typeSchema())
                .addProperty("title", new StringSchema()
                        .description("Título fixo da categoria."))
                .addProperty("status", new IntegerSchema()
                        .description("Status HTTP, repetido no corpo."))
                .addProperty("detail", new StringSchema()
                        .description("Explicação escrita para o usuário. Nunca traz detalhe interno da aplicação."))
                .addProperty("instance", new StringSchema().format("uri")
                        .description("Caminho da requisição que falhou.")
                        .example("/api/v1/users/7295577e-afe6-4875-8bbf-d21c21860711"))
                .addProperty("timestamp", new StringSchema().format("date-time")
                        .description("Instante do erro.")
                        .example("2026-09-25T17:10:42.118"))
                .addProperty("errors", new MapSchema()
                        .additionalProperties(new ArraySchema().items(new StringSchema()))
                        .description("Presente só em falha de validação dos campos: mensagens por campo.")
                        .example(Map.of("email", List.of("deve ser um endereço de e-mail bem formado"))));
        schema.setRequired(List.of("type", "title", "status", "detail", "instance", "timestamp"));
        return schema;
    }
}
