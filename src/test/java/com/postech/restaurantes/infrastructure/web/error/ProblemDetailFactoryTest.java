package com.postech.restaurantes.infrastructure.web.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ProblemDetailFactoryTest {

    static final LocalDateTime AGORA = LocalDateTime.of(2026, 3, 10, 12, 0);
    static final Clock RELOGIO = Clock.fixed(AGORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final ProblemDetailFactory factory = new ProblemDetailFactory(RELOGIO);

    @Test
    @DisplayName("Monta o problema com o type, o título e o status da categoria, o detalhe e o instante")
    void deveMontarOProblemaDaCategoria() {
        ProblemDetail problem = factory.create(ProblemType.RESOURCE_NOT_FOUND, "Usuário não encontrado");

        assertEquals(URI.create("urn:restaurantes:problema:recurso-nao-encontrado"), problem.getType());
        assertEquals("Recurso não encontrado", problem.getTitle());
        assertEquals(404, problem.getStatus());
        assertEquals("Usuário não encontrado", problem.getDetail());
        assertEquals(AGORA, problem.getProperties().get(ProblemDetailFactory.TIMESTAMP));
        assertNull(problem.getInstance(), "nas respostas do Spring MVC o próprio framework preenche");
    }

    @Test
    @DisplayName("Fora do Spring MVC, o caminho da requisição vira o instance")
    void deveRegistrarOCaminhoQuandoInformado() {
        ProblemDetail problem = factory.create(ProblemType.UNAUTHENTICATED, "detalhe", "/api/v1/users");

        assertEquals(URI.create("/api/v1/users"), problem.getInstance());
        assertEquals(401, problem.getStatus());
    }

    @Test
    @DisplayName("Carimba o instante nos problemas que o Spring monta sozinho")
    void deveCarimbarProblemaDoSpring() {
        ProblemDetail doSpring = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);

        factory.stamp(doSpring);

        assertEquals(AGORA, doSpring.getProperties().get(ProblemDetailFactory.TIMESTAMP));
    }

    @Test
    @DisplayName("Carimba também quando já há outras propriedades, mas não o instante")
    void deveCarimbarQuandoFaltaSoOInstante() {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setProperty("outra", "valor");

        factory.stamp(problem);

        assertEquals(AGORA, problem.getProperties().get(ProblemDetailFactory.TIMESTAMP));
        assertEquals("valor", problem.getProperties().get("outra"));
    }

    @Test
    @DisplayName("Não sobrescreve o instante de um problema já carimbado")
    void deveManterOInstanteExistente() {
        ProblemDetail problem = factory.create(ProblemType.DATA_CONFLICT, "conflito");
        LocalDateTime original = (LocalDateTime) problem.getProperties().get(ProblemDetailFactory.TIMESTAMP);

        new ProblemDetailFactory(Clock.offset(RELOGIO, java.time.Duration.ofHours(1))).stamp(problem);

        assertEquals(original, problem.getProperties().get(ProblemDetailFactory.TIMESTAMP));
    }

    @Test
    @DisplayName("Cada categoria tem type próprio: é o identificador em que o cliente se apoia")
    void deveTerTypeUnicoPorCategoria() {
        Set<URI> types = Arrays.stream(ProblemType.values()).map(ProblemType::type).collect(Collectors.toSet());

        assertEquals(ProblemType.values().length, types.size());
        assertTrue(types.stream().allMatch(type -> type.toString().startsWith(ProblemType.URN_PREFIX)));
    }
}
