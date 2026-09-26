package com.postech.restaurantes.infrastructure.web.error;

import java.net.URI;
import org.springframework.http.HttpStatus;

/**
 * Catálogo das categorias de problema da API (RFC 9457, antiga 7807). Cada categoria tem um
 * {@code type} próprio, um título fixo e um status.
 *
 * <p>O {@code type} é o <strong>identificador</strong> que o cliente deve usar para decidir o
 * que fazer — o título pode ser reescrito, o {@code type} não. É uma URN, e não uma URL,
 * de propósito: a RFC permite identificadores não resolvíveis, e uma URL que não leva a lugar
 * nenhum prometeria uma documentação que não existe.
 */
public enum ProblemType {

    INVALID_REQUEST("requisicao-invalida", "Requisição inválida", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD("senha-invalida", "Senha inválida", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN("token-invalido", "Token inválido ou expirado", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_FAILED("falha-na-autenticacao", "Falha na autenticação", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED("nao-autenticado", "Não autenticado", HttpStatus.UNAUTHORIZED),
    FORBIDDEN_OPERATION("operacao-nao-permitida", "Operação não permitida", HttpStatus.FORBIDDEN),
    ACCESS_DENIED("acesso-negado", "Acesso negado", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("recurso-nao-encontrado", "Recurso não encontrado", HttpStatus.NOT_FOUND),
    DATA_CONFLICT("conflito-de-dados", "Conflito de dados", HttpStatus.CONFLICT),
    UNEXPECTED_ERROR("erro-inesperado", "Erro inesperado", HttpStatus.INTERNAL_SERVER_ERROR);

    static final String URN_PREFIX = "urn:restaurantes:problema:";

    private final URI type;
    private final String title;
    private final HttpStatus status;

    ProblemType(String slug, String title, HttpStatus status) {
        this.type = URI.create(URN_PREFIX + slug);
        this.title = title;
        this.status = status;
    }

    public URI type() {
        return type;
    }

    public String title() {
        return title;
    }

    public HttpStatus status() {
        return status;
    }
}
