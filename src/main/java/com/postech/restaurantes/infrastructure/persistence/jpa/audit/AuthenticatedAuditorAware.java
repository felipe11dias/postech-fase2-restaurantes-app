package com.postech.restaurantes.infrastructure.persistence.jpa.audit;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.data.domain.AuditorAware;

/**
 * Quem está gravando. Responde ao {@code AuditingEntityListener} com o login autenticado ou
 * com {@value #SYSTEM} quando não há ninguém — requisição pública, migration ou seed.
 *
 * <p>Não sabe <em>como</em> se descobre o usuário autenticado: recebe essa resposta pronta, como
 * um {@link Supplier}, ligado pelo módulo {@code main}. Assim o módulo JPA não depende do
 * mecanismo de autenticação, e trocar um não obriga a mexer no outro.
 */
public class AuthenticatedAuditorAware implements AuditorAware<String> {

    /** Autor registrado quando a operação não parte de um usuário autenticado. */
    public static final String SYSTEM = "system";

    private final Supplier<Optional<String>> currentLogin;

    public AuthenticatedAuditorAware(Supplier<Optional<String>> currentLogin) {
        this.currentLogin = Objects.requireNonNull(currentLogin, "Fonte do autor corrente é obrigatória");
    }

    @Override
    public Optional<String> getCurrentAuditor() {
        return currentLogin.get().or(() -> Optional.of(SYSTEM));
    }
}
