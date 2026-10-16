package com.postech.restaurantes;

import java.util.Collection;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Consulta de apoio dos testes de integração: o endereço não é exposto por conta própria na API, e
 * o banco não impede que ele sobre — as chaves estrangeiras apontam <em>para</em> {@code addresses}.
 * Quem garante que nada sobra é a aplicação; o teste confere contando os endereços que <em>ele</em>
 * criou, nunca a tabela inteira, porque o banco é compartilhado entre as classes de teste.
 */
public final class EnderecosNoBanco {

    private EnderecosNoBanco() {
    }

    /** Quantos dos endereços informados ainda existem em {@code addresses}. */
    public static int existentes(JdbcTemplate jdbc, Collection<UUID> ids) {
        return ids.stream()
                .mapToInt(id -> jdbc.queryForObject("SELECT count(*) FROM addresses WHERE id = ?", Integer.class, id))
                .sum();
    }
}
