package com.postech.restaurantes.infrastructure.api.rest.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.postech.restaurantes.WebIntegrationTestSupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Horário de funcionamento por dia (Etapa 24), por HTTP de verdade: vários intervalos, em ordem da semana;
 * sobreposição e dia desconhecido recusados com a mensagem do domínio; e a atualização que mantém na mesma
 * linha o horário que continua.
 */
class RestaurantOfficeHoursIT extends WebIntegrationTestSupport {

    private static final String RESTAURANTS = "/api/v1/restaurants";

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("Cadastro com horários diferentes por dia: a resposta traz todos, na ordem da semana")
    void deveCadastrarHorariosPorDia() {
        String dono = donoAutenticado();

        ResponseEntity<JsonNode> criado = criar(dono, List.of(
                horario("SUNDAY", "10:00:00", "15:00:00"),
                horario("friday", "18:00:00", "02:00:00"),
                horario("MONDAY", "18:00:00", "23:00:00"),
                horario("MONDAY", "11:00:00", "15:00:00")));

        assertEquals(HttpStatus.CREATED, criado.getStatusCode(), String.valueOf(criado.getBody()));
        JsonNode horarios = criado.getBody().get("officeHours");
        assertEquals(4, horarios.size());
        assertEquals("MONDAY 11:00:00", dia(horarios.get(0)));
        assertEquals("MONDAY 18:00:00", dia(horarios.get(1)));
        assertEquals("FRIDAY 18:00:00", dia(horarios.get(2)));
        assertEquals("02:00:00", horarios.get(2).get("endTime").asText(), "fechamento depois da meia-noite");
        assertEquals("SUNDAY 10:00:00", dia(horarios.get(3)));
        assertEquals(4, contar(UUID.fromString(criado.getBody().get("id").asText()), null));
    }

    @Test
    @DisplayName("Horários sobrepostos (inclusive o que vira a meia-noite), dia desconhecido e nenhum horário: 400")
    void deveRecusarHorariosInvalidos() {
        String dono = donoAutenticado();

        ResponseEntity<JsonNode> sobrepostos = criar(dono, List.of(
                horario("MONDAY", "22:00:00", "02:00:00"),
                horario("TUESDAY", "01:00:00", "10:00:00")));
        ResponseEntity<JsonNode> diaDesconhecido = criar(dono, List.of(horario("FERIADO", "08:00:00", "12:00:00")));
        ResponseEntity<JsonNode> semHorario = criar(dono, List.of());

        assertEquals(HttpStatus.BAD_REQUEST, sobrepostos.getStatusCode());
        assertEquals("Os horários de funcionamento não podem se sobrepor", sobrepostos.getBody().get("detail").asText());
        assertEquals("Dia da semana inválido: FERIADO", diaDesconhecido.getBody().get("detail").asText());
        assertEquals("Restaurante deve ter ao menos um horário de funcionamento",
                semHorario.getBody().get("detail").asText());
    }

    @Test
    @DisplayName("Atualização mantém na mesma linha o horário que continua, cria o novo e tira o ausente")
    void deveAtualizarOsHorarios() {
        String dono = donoAutenticado();
        JsonNode criado = criar(dono, List.of(
                horario("MONDAY", "08:00:00", "12:00:00"),
                horario("TUESDAY", "08:00:00", "12:00:00"))).getBody();
        UUID restaurante = UUID.fromString(criado.get("id").asText());
        UUID linhaDeSegunda = idDoHorario(restaurante, "MONDAY");

        ResponseEntity<JsonNode> atualizado = rest.exchange(RESTAURANTS + "/" + restaurante, HttpMethod.PUT,
                corpoAutenticado(restaurante(List.of(
                        horario("MONDAY", "08:00:00", "14:00:00"),
                        horario("WEDNESDAY", "09:00:00", "17:00:00"))), dono), JsonNode.class);

        assertEquals(HttpStatus.OK, atualizado.getStatusCode(), String.valueOf(atualizado.getBody()));
        assertEquals("14:00:00", atualizado.getBody().at("/officeHours/0/endTime").asText());
        assertEquals(linhaDeSegunda, idDoHorario(restaurante, "MONDAY"), "o horário que continua fica na mesma linha");
        assertEquals(0, contar(restaurante, "TUESDAY"));
        assertEquals(2, contar(restaurante, null));
    }

    private ResponseEntity<JsonNode> criar(String token, List<Map<String, Object>> horarios) {
        return rest.exchange(RESTAURANTS, HttpMethod.POST, corpoAutenticado(restaurante(horarios), token),
                JsonNode.class);
    }

    private int contar(UUID restaurante, String dia) {
        return dia == null
                ? jdbc.queryForObject("SELECT count(*) FROM restaurant_office_hours WHERE restaurant_id = ?",
                        Integer.class, restaurante)
                : jdbc.queryForObject("SELECT count(*) FROM restaurant_office_hours WHERE restaurant_id = ? "
                        + "AND day_of_week = ?::day_of_week", Integer.class, restaurante, dia);
    }

    private UUID idDoHorario(UUID restaurante, String dia) {
        return jdbc.queryForObject("SELECT id FROM restaurant_office_hours WHERE restaurant_id = ? "
                + "AND day_of_week = ?::day_of_week", UUID.class, restaurante, dia);
    }

    private String donoAutenticado() {
        String login = "horario." + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<JsonNode> criado = rest.postForEntity("/api/v1/users", corpo(Map.of("name", "Dono " + login,
                "email", login + "@email.com", "login", login, "password", "senhaSegura123",
                "owner", perfilDeDono())), JsonNode.class);
        assertEquals(HttpStatus.CREATED, criado.getStatusCode(), String.valueOf(criado.getBody()));
        return autenticar(login, "senhaSegura123");
    }

    private static Map<String, Object> restaurante(List<Map<String, Object>> horarios) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("name", "Restaurante dos Horários");
        corpo.put("officeHours", horarios);
        corpo.put("address", Map.of("street", "Rua do Relógio", "number", "1", "neighborhood", "Centro",
                "city", "São Paulo", "state", "SP", "zipCode", "01001000"));
        return corpo;
    }

    private static Map<String, Object> horario(String dia, String abre, String fecha) {
        return Map.of("dayOfWeek", dia, "startTime", abre, "endTime", fecha);
    }

    private static String dia(JsonNode horario) {
        return horario.get("dayOfWeek").asText() + " " + horario.get("startTime").asText();
    }
}
