package com.postech.restaurantes.adapter.service.data;

import java.util.Set;
import java.util.UUID;

/** O que vai dentro do token de acesso: quem é o portador, pelo id e pelo login, e seus papéis. */
public record TokenClaimsData(UUID subject, String login, Set<String> roles) {
}
